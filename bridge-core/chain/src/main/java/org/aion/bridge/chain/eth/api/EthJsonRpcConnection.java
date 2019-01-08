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

package org.aion.bridge.chain.eth.api;

import org.aion.bridge.chain.aion.rpc.dto.CallContract;
import org.aion.bridge.chain.base.api.*;
import org.aion.bridge.chain.base.rpc.JsonRpcError;
import org.aion.bridge.chain.base.rpc.dto.GetBlockByNumber;
import org.aion.bridge.chain.base.rpc.dto.GetPeerCount;
import org.aion.bridge.chain.base.rpc.dto.GetTransactionReceipt;
import org.aion.bridge.chain.base.types.BlockWithReceipts;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.eth.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

public class EthJsonRpcConnection extends JsonRpcConnectionBase<EthBlock, EthReceipt, EthLog, EthAddress>
        implements StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> {

    private final Logger log = LoggerFactory.getLogger("RPC_BASE");

    public EthJsonRpcConnection(String connection, Long timeoutSeconds) {
        super(connection, timeoutSeconds);
    }

    public EthJsonRpcConnection(String connection) {
        super(connection);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public List<BlockWithReceipts<EthBlock, EthReceipt, EthLog>> getReceiptsForBlocks(List<EthBlock> blocks)
            throws IncompleteApiCallException, MalformedApiResponseException {

        // generate the response object and return
        List<BlockWithReceipts<EthBlock, EthReceipt, EthLog>> response = new ArrayList<>();
        if (blocks.size() == 0)
            return response;

        // populate the set of requests we need to send out
        List<GetTransactionReceipt.Request> requests = new ArrayList<>();
        Set<Word32> paranoidTxHashList = new HashSet<>();
        for (EthBlock block : blocks) {
            for (Word32 txHash : block.getTransactionHashes()) {
                requests.add(new GetTransactionReceipt.Request(txHash));
                paranoidTxHashList.add(txHash);
            }
        }

        // create an unmodifiable view on the requests, since we should no longer be touching this data structure
        requests = Collections.unmodifiableList(requests);

        log.trace("url=[{}] method=[getReceiptsForBlocks] Query for [{}] receipts from [{}] blocks",
                provider.getUrl(), requests.size(), blocks.size());

        // don't send an empty request out to api
        if (requests.size() == 0) {
            for (EthBlock block : blocks) {
                response.add(new BlockWithReceipts<>(block, Collections.emptyList()));
            }
            return response;
        }

        // store all the responses from API here. maps block hash to list of receipts
        Map<Word32, List<EthReceipt>> receiptsMap = new HashMap<>();

        List<GetTransactionReceipt.Response> receiptResponses = provider.sendBatch(requests, GetTransactionReceipt.Response.class);

        if (receiptResponses.size() != requests.size()) {
            log.error("url=[{}] method=[getReceiptsForBlocks] Response is missing blocks; expected=[{}] observed=[{}]",
                    provider.getUrl(), requests.size(), receiptResponses.size());

            throw new MalformedApiResponseException("method=[getReceiptsForBlocks] Missing receipts in response (count mismatch)");
        }

        for (GetTransactionReceipt.Response r : receiptResponses) {
            if (r == null || r.getError() != null) {
                String dump = "No request dump available";
                //noinspection CatchMayIgnoreException
                try {
                    dump = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(receiptResponses);
                } catch (Exception e) {
                }

                log.error("url=[{}] method=[getReceiptsForBlocks] Receipt not available or in-error: \n{}",
                        provider.getUrl(), dump);

                throw new MalformedApiResponseException("method=[getReceiptsForBlocks] Receipt not available or in-error");
            }

            GetTransactionReceipt.TransactionReceiptJson receiptJson = r.getTransactionReceipt();

            EthReceipt resultReceipt = receiptFromResponseType(receiptJson);

            // update our receiptsMaps
            List<EthReceipt> receipts = receiptsMap.get(resultReceipt.getBlockHash());
            if (receipts == null)
                receiptsMap.put(resultReceipt.getBlockHash(), newArrayList(resultReceipt));
            else
                receipts.add(resultReceipt);

            // paranoid check
            paranoidTxHashList.remove(resultReceipt.getTransactionHash());
        }

        // make sure we got all the transactions we asked for
        if (!paranoidTxHashList.isEmpty()) {
            String dump = "No paranoid transaction hash list dump available";
            //noinspection CatchMayIgnoreException
            try {
                dump = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(paranoidTxHashList);
            } catch (Exception e) {
            }

            log.error("url=[{}] method=[getReceiptsForBlocks] Paranoid transaction hash list not empty at end of processing: \n{}",
                    provider.getUrl(), dump);

            throw new MalformedApiResponseException("method=[getReceiptsForBlocks] Paranoid transaction hash list not empty at end of processing");
        }

        for (EthBlock block : blocks) {
            response.add(new BlockWithReceipts<>(block, receiptsMap.get(block.getHash())));
        }
        return response;
    }

    @SuppressWarnings("Duplicates")
    public Optional<EthReceipt> getReceipt(Word32 transactionHash)
            throws IncompleteApiCallException, MalformedApiResponseException {
        GetTransactionReceipt.Request request = new GetTransactionReceipt.Request(transactionHash);

        GetTransactionReceipt.Response response = provider.send(request, GetTransactionReceipt.Response.class);

        JsonRpcError error = response.getError();
        if (error != null) {
            String errStr = "";
            try {
                errStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(error);
            } catch (Exception e) {
                log.error("Could not convert error object to String", e);
            }

            throw new MalformedApiResponseException("url=[" + provider.getUrl() + "] " +
                    "method=[getReceipt] " +
                    "RPC error non-null.\n" + errStr);
        }

        GetTransactionReceipt.TransactionReceiptJson receiptJson = response.getTransactionReceipt();

        // we can get null receipts for a transaction hash
        if (receiptJson == null) return Optional.empty();

        EthReceipt receipt = receiptFromResponseType(receiptJson);

        return Optional.of(receipt);
    }

    @Override
    public BigInteger getNonce(EthAddress address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Word32 sendRawTransaction(ImmutableBytes rawTransaction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigInteger getGasPrice() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String contractCall(EthAddress address, ImmutableBytes abi, String blockId)
            throws IncompleteApiCallException, MalformedApiResponseException  {
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
    public String contractCall(EthAddress address, ImmutableBytes abi) throws MalformedApiResponseException, IncompleteApiCallException {
        return contractCall(address, abi, "latest");
    }

    @Override
    public BigInteger getBalance(EthAddress address, String blockId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer peerCount() throws IncompleteApiCallException, MalformedApiResponseException {
        GetPeerCount.Request req = new GetPeerCount.Request();
        GetPeerCount.Response response = provider.send(req, GetPeerCount.Response.class);
        JsonRpcError error = response.getError();
        if (response.getError() != null) {
            String errStr = "";
            try {
                errStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(error);
            } catch (Exception e) {
                log.error("Could not convert error object to String", e);
            }

            throw new MalformedApiResponseException("url=[" + provider.getUrl() + "] " +
                    "method=[net_peerCount] " +
                    "RPC error non-null.\n" + errStr);
        }
        return response.getCount();
    }

    @Override
    protected EthBlock blockFromResponseType(GetBlockByNumber.BlockJson blockJson) {
        return new EthBlock(
                blockJson.getBlockNumber().toBigInteger().longValueExact(),
                new Word32(blockJson.getHash()),
                new Word32(blockJson.getParentHash()),
                new KeccakBloom(blockJson.getLogsBloom()),
                blockJson.getTotalDifficulty().toBigInteger(),
                blockJson.getTimestamp().toBigInteger().longValueExact(),
                blockJson.getTransactionHashes().stream().map(Word32::new).collect(toList()));
    }

    @Override
    protected EthReceipt receiptFromResponseType(GetTransactionReceipt.TransactionReceiptJson receiptJson) {
        return new EthReceipt(
                receiptJson.getBlockNumber().toBigInteger().longValueExact(),
                receiptJson.getTransactionHash(),
                receiptJson.getBlockHash(),
                new EthAddress(receiptJson.getFrom()),
                receiptJson.getTo() == null ? null : new EthAddress(receiptJson.getTo()),
                new KeccakBloom(receiptJson.getLogsBloom().getByteArray()),
                receiptJson.getLogs().stream()
                        .map(l -> new EthLog(new EthAddress(l.getAddress()),
                                l.getData(),
                                l.getTopics()))
                        .collect(toList()),
                receiptJson.getStatus(),
                receiptJson.getTransactionIndex().toBigInteger().intValueExact());
    }

    @Override
    public void evictConnections() {
        provider.evictConnections();
    }
}