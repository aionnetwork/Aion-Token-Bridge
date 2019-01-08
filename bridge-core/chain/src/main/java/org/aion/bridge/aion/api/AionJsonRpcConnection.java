/*
 * This code is licensed under the MIT License
 *
 * Copyright (c) 2019 Aion Foundation https://aion.network/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.aion.bridge.chain.aion.api;

import com.google.common.base.Stopwatch;
import org.aion.bridge.chain.aion.rpc.dto.*;
import org.aion.bridge.chain.aion.types.*;
import org.aion.bridge.chain.base.api.*;
import org.aion.bridge.chain.base.rpc.JsonRpcError;
import org.aion.bridge.chain.base.rpc.dto.GetBlockByNumber;
import org.aion.bridge.chain.base.rpc.dto.GetTransactionReceipt;
import org.aion.bridge.chain.base.types.BlockWithReceipts;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class AionJsonRpcConnection extends JsonRpcConnectionBase<AionBlock, AionReceipt, AionLog, AionAddress>
        implements StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> {

    private final Logger log = LoggerFactory.getLogger(AionJsonRpcConnection.class);

    public AionJsonRpcConnection(String connection, Long timeoutSeconds) {
        super(connection, timeoutSeconds);
    }

    public AionJsonRpcConnection(String connection) {
        super(connection);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public List<BlockWithReceipts<AionBlock, AionReceipt, AionLog>> getReceiptsForBlocks(List<AionBlock> blocks)
            throws IncompleteApiCallException, MalformedApiResponseException {

        // generate the response object and return
        List<BlockWithReceipts<AionBlock, AionReceipt, AionLog>> response = new ArrayList<>();
        if (blocks.size() == 0)
            return response;

        // populate the set of requests we need to send out
        List<GetTransactionReceiptsByBlockHash.Request> requests = new ArrayList<>();
        Set<Word32> requestedTxHashes = new HashSet<>();
        Map<Word32, AionBlock> requestedBlocks = new HashMap<>();
        for (AionBlock block : blocks) {
            List<Word32> transactionHashes = block.getTransactionHashes();
            if (transactionHashes.size() > 0) {
                requests.add(new GetTransactionReceiptsByBlockHash.Request(block.getHash()));
                requestedTxHashes.addAll(block.getTransactionHashes());
                requestedBlocks.put(block.getHash(), block);
            } else {
                response.add(new BlockWithReceipts<>(block, Collections.emptyList()));
            }
        }

        // don't send an empty request out to api
        if (requests.size() == 0)
            return response;

        // create an unmodifiable view on the requests, since we should no longer be touching this data structure
        requests = Collections.unmodifiableList(requests);

        log.trace("url=[{}] method=[getReceiptsForBlocks] Query for [{}] receipts from [{}] blocks",
                provider.getUrl(), requestedTxHashes.size(), blocks.size());

        List<GetTransactionReceiptsByBlockHash.Response> blockResponses =
                provider.sendBatch(requests, GetTransactionReceiptsByBlockHash.Response.class);

        if (blockResponses.size() != requests.size()) {
            log.error("url=[{}] method=[getReceiptsForBlocks] Response is missing blocks; expected=[{}] observed=[{}]",
                    provider.getUrl(), requests.size(), blockResponses.size());

            throw new MalformedApiResponseException("method=[getReceiptsForBlocks] Missing receipts in response (count mismatch)");
        }

        for (GetTransactionReceiptsByBlockHash.Response r : blockResponses) {
            if (r == null || r.getError() != null) {
                String dump = "No request dump available";
                //noinspection CatchMayIgnoreException
                try {
                    dump = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(blockResponses);
                } catch (Exception e) { }

                log.error("url=[{}] method=[getReceiptsForBlocks] Receipt not available or in-error: \n{}",
                        provider.getUrl(), dump);

                throw new MalformedApiResponseException("method=[getReceiptsForBlocks] Receipt not available or in-error");
            }

            List<GetTransactionReceipt.TransactionReceiptJson> receiptJsonList = r.getTransactionReceiptList();
            List<AionReceipt> receipts = new ArrayList<>();
            Set<Word32> paranoidBlockHashSet = new HashSet<>();

            for (GetTransactionReceipt.TransactionReceiptJson receiptJson : receiptJsonList) {
                AionReceipt receipt = receiptFromResponseType(receiptJson);
                receipts.add(receipt);
                requestedTxHashes.remove(receipt.getTransactionHash());
                paranoidBlockHashSet.add(receipt.getBlockHash());
            }

            if (paranoidBlockHashSet.size() != 1)
                throw new IllegalStateException("paranoidBlockHashSet should have only one block hash");

            AionBlock block = requestedBlocks.remove(paranoidBlockHashSet.iterator().next());

            response.add(new BlockWithReceipts<>(block, receipts));
        }

        // make sure we got all the transactions we asked for
        if (!requestedTxHashes.isEmpty()) {
            String dump = "No paranoid transaction hash list dump available";
            //noinspection CatchMayIgnoreException
            try {
                dump = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestedTxHashes);
            } catch (Exception e) { }

            log.error("url=[{}] method=[getReceiptsForBlocks] Paranoid transaction hash list not empty at end of processing: \n{}",
                    provider.getUrl(), dump);

            throw new MalformedApiResponseException("method=[getReceiptsForBlocks] Paranoid transaction hash list not empty at end of processing");
        }

        return response;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public Optional<AionReceipt> getReceipt(Word32 transactionHash)
            throws IncompleteApiCallException, MalformedApiResponseException {
        GetTransactionReceiptOps.Request request = new GetTransactionReceiptOps.Request(transactionHash);

        GetTransactionReceiptOps.Response response = provider.send(request, GetTransactionReceiptOps.Response.class);

        JsonRpcError error = response.getError();
        if (error != null) {
            String errStr = "";
            try {
                errStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(error);
            } catch (Exception e) {
                log.error("Could not convert error object to String", e);
            }

            throw new MalformedApiResponseException("url=["+provider.getUrl()+"] "+
                    "method=[getReceipt] "+
                    "RPC error non-null.\n" +errStr);
        }

        GetTransactionReceipt.TransactionReceiptJson receiptJson = response.getTransactionReceipt();

        // we can get null receipts for a transaction hash
        if (receiptJson == null) return Optional.empty();

        AionReceipt receipt = receiptFromResponseType(receiptJson);

        return Optional.of(receipt);
    }

    @Override
    public BigInteger getNonce(AionAddress address)
            throws IncompleteApiCallException, MalformedApiResponseException {
        GetTransactionCount.Request request = new GetTransactionCount.Request(address, "latest");

        Stopwatch stopwatch = Stopwatch.createStarted();
        GetTransactionCount.Response response = this.provider.send(request, GetTransactionCount.Response.class);
        stopwatch.stop();
        if (response == null || response.getError() != null) {
            throw new MalformedApiResponseException("getNonce response null or in-error");
        }

        log.info("Received nonce: {} in {}", response.getNumericalValue().toBigInteger(), stopwatch.toString());

        return response.getNumericalValue().toBigInteger();
    }

    @Override
    public BigInteger getGasPrice()
            throws IncompleteApiCallException, MalformedApiResponseException {

        Stopwatch stopwatch = Stopwatch.createStarted();
        GetGasPrice.Response response = provider.send(GetGasPrice.INSTANCE, GetGasPrice.Response.class);
        stopwatch.stop();
        if (response == null || response.getError() != null)
            throw new MalformedApiResponseException("getGasPrice response null or in-error");
        return response.getGasPrice();
    }

    @Override
    public String contractCall(AionAddress address, ImmutableBytes abi, String blockId)
            throws IncompleteApiCallException, MalformedApiResponseException {

        CallContract.Request request;
        if (blockId == null)
            request = new CallContract.Request(address, abi);
        else
            request = new CallContract.Request(address, abi, blockId);

        CallContract.Response response = provider.send(request, CallContract.Response.class);

        if (response == null || response.getError() != null)
            throw new MalformedApiResponseException("contractCall response null or in-error");

        return response.getResult();
    }

    @Override
    public String contractCall(AionAddress address, ImmutableBytes abi)
            throws IncompleteApiCallException, MalformedApiResponseException {
        return contractCall(address, abi, "latest");
    }

    @Override
    public BigInteger getBalance(AionAddress address, String blockId)
            throws IncompleteApiCallException, MalformedApiResponseException {
        GetBalance.Request request = new GetBalance.Request(address);

        Stopwatch stopwatch = Stopwatch.createStarted();
        GetBalance.Response response = this.provider.send(request, GetBalance.Response.class);
        if (response == null || response.getError() != null)
            throw new MalformedApiResponseException("getBalance response null or in-error");

        log.info("Received balance of {} Aion for {} in {}", response.getNumericalValue().toBigInteger().doubleValue() / (BigInteger.TEN.pow(18).doubleValue()),
                address.toString(), stopwatch.toString());

        return response.getNumericalValue().toBigInteger();
    }

    @Override
    public Integer peerCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Word32 sendRawTransaction(ImmutableBytes transaction)
            throws IncompleteApiCallException, MalformedApiResponseException {
        SendRawTransaction.Request request = new SendRawTransaction.Request(transaction);

        Stopwatch stopwatch = Stopwatch.createStarted();
        SendRawTransaction.Response response = provider.send(request, SendRawTransaction.Response.class);
        stopwatch.stop();
        if (response == null || response.getError() != null)
            throw new MalformedApiResponseException("sendRawTransaction response null or in-error");

        log.info("Sent raw Tx: {} in {}", transaction.toString(), stopwatch.toString());

        return response.getTransactionHash();
    }

    @Override
    protected AionBlock blockFromResponseType(GetBlockByNumber.BlockJson blockJson) {
        return new AionBlock(
                blockJson.getBlockNumber().toBigInteger().longValueExact(),
                new Word32(blockJson.getHash()),
                new Word32(blockJson.getParentHash()),
                new BlakeBloom(blockJson.getLogsBloom()),
                blockJson.getTotalDifficulty().toBigInteger(),
                blockJson.getTimestamp().toBigInteger().longValueExact(),
                blockJson.getTransactionHashes().stream().map(Word32::new).collect(Collectors.toList()));
    }

    @Override
    protected AionReceipt receiptFromResponseType(GetTransactionReceipt.TransactionReceiptJson receiptJson) {
        return new AionReceipt(
                receiptJson.getBlockNumber().toBigInteger().longValueExact(),
                receiptJson.getTransactionHash(),
                receiptJson.getBlockHash(),
                new AionAddress(receiptJson.getFrom()),
                receiptJson.getTo().replace("0x", "").length() == 0 ? null : new AionAddress(receiptJson.getTo()),
                new BlakeBloom(receiptJson.getLogsBloom().getByteArray()),
                receiptJson.getLogs().stream()
                        .map(l -> new AionLog(new Word32(l.getAddress())
                                , l.getData(),
                                l.getTopics()))
                        .collect(Collectors.toList()),
                receiptJson.getStatus(),
                receiptJson.getTransactionIndex().toBigInteger().intValueExact());
    }

    @Override
    public void evictConnections() {
        provider.evictConnections();
    }

}
