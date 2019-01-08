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

package org.aion.bridge.chain.base.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aion.bridge.chain.base.rpc.JsonRpcError;
import org.aion.bridge.chain.base.rpc.dto.GetBlockByNumber;
import org.aion.bridge.chain.base.rpc.dto.GetBlockNumber;
import org.aion.bridge.chain.base.rpc.dto.GetTransactionReceipt;
import org.aion.bridge.chain.base.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

public abstract class JsonRpcConnectionBase<B extends Block, R extends Receipt<L>, L extends Log, A extends Address>
        implements StatelessChainConnectionBase<B, R, L, A> {

    private final Logger log = LoggerFactory.getLogger("RPC_BASE");
    protected final JsonRpcProvider provider;
    protected final ObjectMapper mapper;

    public JsonRpcConnectionBase(String connection, Long timeoutSeconds) {
        provider = new JsonRpcProvider(connection, timeoutSeconds);
        mapper = new ObjectMapper();
    }

    public JsonRpcConnectionBase(String connection) {
        provider = new JsonRpcProvider(connection);
        mapper = new ObjectMapper();
    }

    protected abstract B blockFromResponseType(GetBlockByNumber.BlockJson blockJson);

    protected abstract R receiptFromResponseType(GetTransactionReceipt.TransactionReceiptJson receiptJson);

    @Override
    public abstract List<BlockWithReceipts<B, R, L>> getReceiptsForBlocks(List<B> blocks)
            throws IncompleteApiCallException, MalformedApiResponseException;

    @Override
    public Optional<B> getBlock(long blockNumber)
            throws IncompleteApiCallException, MalformedApiResponseException {
        GetBlockByNumber.Request request = new GetBlockByNumber.Request(blockNumber);
        GetBlockByNumber.Response response = provider.send(request, GetBlockByNumber.Response.class);

        JsonRpcError error = response.getError();
        if (error != null) {
            throw new MalformedApiResponseException("url=["+provider.getUrl()+"] "+
                    "method=[getBlock] "+
                    "RPC error non-null. " +
                    "msg: [" + error.getMessage() + "], " +
                    "code: [" + error.getCode() + "], " +
                    "data: [" + error.getData() + "]");
        }

        GetBlockByNumber.BlockJson blockJson = response.getResult();

        // block can be null
        if (blockJson == null) return Optional.empty();

        // not using streams
        B block = blockFromResponseType(blockJson);

        return Optional.of(block);
    }

    @Override
    public Long getBlockNumber()
            throws IncompleteApiCallException, MalformedApiResponseException {

        GetBlockNumber.Response response = provider.send(GetBlockNumber.INSTANCE, GetBlockNumber.Response.class);

        JsonRpcError error = response.getError();
        if (error != null) {
            String errStr = "";
            try {
                errStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(error);
            } catch (Exception e) {
                log.error("Could not convert error object to String", e);
            }

            throw new MalformedApiResponseException("url=["+provider.getUrl()+"] "+
                    "method=[getBlockNumber] "+
                    "RPC error non-null.\n" +errStr);
        }

        BigInteger blockNumber = response.getNumber();
        if (blockNumber == null)
            throw new MalformedApiResponseException("url=["+provider.getUrl()+"] "+
                    "method=[getBlockNumber] "+
                    "Null blockNumber from API");

        return blockNumber.longValueExact();
    }

    @Override
    public abstract Optional<R> getReceipt(Word32 transactionHash)
            throws IncompleteApiCallException, MalformedApiResponseException;

    @Override
    public List<B> getBlocksRangeClosed(long start, long end)
            throws IncompleteApiCallException, MalformedApiResponseException {

        if (start > end) {
            log.error("url=[{}] method=[getBlocksRangeClosed({},{})] Start > End",
                    provider.getUrl(), start, end);
            throw new IllegalArgumentException("method=[getBlocksRangeClosed] Start > End");
        }

        List<GetBlockByNumber.Request> requests = new ArrayList<>();
        Set<Long> paranoidBlockNumberList = new HashSet<>();

        for (long i = start; i <= end; i++) {
            requests.add(new GetBlockByNumber.Request(BigInteger.valueOf(i)));
            paranoidBlockNumberList.add(i);
        }

        // create an unmodifiable view on the requests, since we should no longer be touching this data structure
        requests = Collections.unmodifiableList(requests);

        log.trace("url=[{}] method=[getBlocksRangeClosed({},{})] Query for [{}] blocks", provider.getUrl(), start, end, requests.size());

        // store all responses from API here
        List<B> blocks = new ArrayList<>();

        List<GetBlockByNumber.Response> blockJsonList = provider.sendBatch(requests, GetBlockByNumber.Response.class);

        if (blockJsonList.size() != requests.size()) {
            log.error("url=[{}] method=[getBlocksRangeClosed({},{})] Response is missing blocks; expected=[{}] observed=[{}]",
                    provider.getUrl(), start, end, requests.size(), blockJsonList.size());

            throw new MalformedApiResponseException("method=[getBlocksRangeClosed] Missing blocks in response (count mismatch)");
        }

        for (GetBlockByNumber.Response r : blockJsonList) {
            if (r == null || r.getError() != null) {
                String dump = "No request dump available";
                //noinspection CatchMayIgnoreException
                try {
                    dump = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(blockJsonList);
                } catch (Exception e) { }

                log.error("url=[{}] method=[getBlocksRangeClosed({},{})] Block not available or in-error: \n{}",
                        provider.getUrl(), start, end, dump);

                throw new MalformedApiResponseException("method=[getBlocksRangeClosed] Block not available or in-error");
            }

            GetBlockByNumber.BlockJson blockJson = r.getResult();

            B block = blockFromResponseType(blockJson);

            blocks.add(block);
            paranoidBlockNumberList.remove(block.getNumber());
        }


        if (!paranoidBlockNumberList.isEmpty()) {
            String dump = "No paranoid block number list dump available";
            //noinspection CatchMayIgnoreException
            try {
                dump = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(paranoidBlockNumberList);
            } catch (Exception e) { }

            log.error("url=[{}] method=[getBlocksRangeClosed({},{})] Paranoid blockNumber list not empty at end of processing: \n{}",
                    provider.getUrl(), start, end, dump);

            throw new MalformedApiResponseException("method=[getBlocksRangeClosed] Paranoid blockNumber list not empty at end of processing");
        }

        blocks.sort(ChainLink::compareTo);
        return blocks;
    }
}