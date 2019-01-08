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

package org.aion.bridge.chain.base.rpc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aion.bridge.chain.aion.rpc.NumericalValue;
import org.aion.bridge.chain.base.rpc.JsonRpcRequest;
import org.aion.bridge.chain.base.rpc.JsonRpcResponse;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

public class GetTransactionReceipt {
    public static class Request extends JsonRpcRequest {

        @JsonProperty("params")
        private String[] params;

        public Request(Word32 transactionHash) {
            super("eth_getTransactionReceipt");
            this.params = new String[1];
            this.params[0] = transactionHash.toStringWithPrefix();
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Log {

        @JsonProperty("logIndex")
        private String logIndex;

        @JsonProperty("blockNumber")
        private String blockNumber;

        @JsonProperty("transactionIndex")
        private String transactionIndex;

        @JsonProperty("address")
        private String address;

        @JsonProperty("data")
        private String data;

        @JsonProperty("topics")
        private List<String> topics;

        private ImmutableBytes dataCache;
        private List<Word32> topicsCache;

        public String getAddress() { return address; }

        @JsonIgnore
        public ImmutableBytes getData() {
            if (dataCache == null)
                dataCache = new ImmutableBytes(data);
            return dataCache;
        }

        @JsonIgnore
        public List<Word32> getTopics() {
            if (topicsCache == null) {
                topicsCache = topics.stream().map(Word32::new).collect(Collectors.toList());
            }
            return topicsCache;
        }

        @JsonIgnore
        public BigInteger getLogIndex() {
            return new NumericalValue(this.logIndex).toBigInteger();
        }

        @JsonIgnore
        public BigInteger getBlockNumber() {
            return new NumericalValue(this.blockNumber).toBigInteger();
        }

        @JsonIgnore
        public BigInteger getTransactionIndex() {
            return new NumericalValue(this.transactionIndex).toBigInteger();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionReceiptJson {
        @JsonProperty("transactionHash")
        private String transactionHash;

        @JsonProperty("transactionIndex")
        private String transactionIndex;

        @JsonProperty("blockHash")
        private String blockHash;

        @JsonProperty("blockNumber")
        private String blockNumber;

        @JsonProperty("cumulativeGasUsed")
        private String cumulativeGasUsed;

        @JsonProperty("gasUsed")
        private String gasUsed;

        @JsonProperty("cumulativeNrgUsed")
        private String cumulativeNrgUsed;

        @JsonProperty("nrgUsed")
        private String nrgUsed;

        @JsonProperty("contractAddress")
        private String contractAddress;

        @JsonProperty("from")
        private String from;

        @JsonProperty("to")
        private String to;

        @JsonProperty("logsBloom")
        private String logsBloom;

        @JsonProperty("status")
        private String status;

        @JsonProperty("nrgPrice")
        private String nrgPrice;

        @JsonProperty("logs")
        private List<Log> logs;

        private Word32 transactionHashCache;
        private NumericalValue transactionIndexCache;
        private Word32 blockHashCache;
        private NumericalValue blockNumberCache;
        private NumericalValue cumulativeGasUsedCache;
        private NumericalValue gasUsedCache;
        private ImmutableBytes logsBloomCache;
        private Boolean statusCache;

        @JsonIgnore
        public Word32 getTransactionHash() {
            if (transactionHashCache == null && transactionHash != null)
                transactionHashCache = new Word32(transactionHash);
            return transactionHashCache;
        }

        @JsonIgnore
        public NumericalValue getTransactionIndex() {
            if (transactionIndexCache == null && transactionIndex != null)
                transactionIndexCache = new NumericalValue(transactionIndex);
            return transactionIndexCache;
        }

        @JsonIgnore
        public Word32 getBlockHash() {
            if (blockHashCache == null && blockHash != null)
                blockHashCache = new Word32(blockHash);
            return blockHashCache;
        }

        @JsonIgnore
        public NumericalValue getBlockNumber() {
            if (blockNumberCache == null && blockNumber != null)
                blockNumberCache = new NumericalValue(blockNumber);
            return blockNumberCache;
        }

        @JsonIgnore
        public NumericalValue getCumulativeGasUsed() {
            if (cumulativeGasUsedCache == null && cumulativeGasUsed != null)
                cumulativeGasUsedCache = new NumericalValue(cumulativeGasUsed);
            return cumulativeGasUsedCache;
        }

        @JsonIgnore
        public NumericalValue getGasUsed() {
            if (gasUsedCache == null && gasUsed != null)
                gasUsedCache = new NumericalValue(gasUsed);
            return gasUsedCache;
        }

        @JsonIgnore
        public NumericalValue getCumulativeNrgUsed() {
            if (cumulativeGasUsedCache == null && cumulativeGasUsed != null)
                cumulativeGasUsedCache = new NumericalValue(cumulativeGasUsed);
            return cumulativeGasUsedCache;
        }

        @JsonIgnore
        public NumericalValue getNrgUsed() {
            if (gasUsedCache == null && gasUsed != null)
                gasUsedCache = new NumericalValue(gasUsed);
            return gasUsedCache;
        }

        public String getContractAddress() { return contractAddress; }
        public String getFrom() { return from; }
        public String getTo() { return to; }

        @JsonIgnore
        public ImmutableBytes getLogsBloom() {
            if (logsBloomCache == null && logsBloom != null)
                logsBloomCache = new ImmutableBytes(logsBloom);
            return logsBloomCache;
        }

        @JsonIgnore
        public Boolean getStatus() {
            if (statusCache == null && status != null)
                statusCache = new NumericalValue(status).toBigInteger().equals(BigInteger.ONE);
            return statusCache;
        }

        @JsonIgnore
        public List<Log> getLogs() {
            return this.logs;
        }

        @Override
        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "failed to decode block json object";
            }
        }
    }

    public static class Response extends JsonRpcResponse {
        @JsonProperty("result")
        private TransactionReceiptJson result;

        public TransactionReceiptJson getTransactionReceipt() {
            return result;
        }
    }
}
