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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aion.bridge.chain.aion.rpc.NumericalValue;
import org.aion.bridge.chain.base.rpc.JsonRpcRequest;
import org.aion.bridge.chain.base.rpc.JsonRpcResponse;
import org.aion.bridge.chain.base.utility.ByteUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.List;

public class GetBlockByNumber {

    public static class Request extends JsonRpcRequest {

        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        private MethodParam params;

        // TODO: this is sloppy, fix it later
        public Request(Object input) {
            super("eth_getBlockByNumber");
            String number = "";
            if (input instanceof String) {
                number = (String) input;
            } else if (input instanceof BigInteger) {
                BigInteger in = (BigInteger) input;
                number = "0x" + StringUtils.stripStart(ByteUtils.binToHex(in.toByteArray()), "0");
            } else if (input instanceof Long) {
                BigInteger in = BigInteger.valueOf((Long) input);
                number = "0x" + StringUtils.stripStart(ByteUtils.binToHex(in.toByteArray()), "0");
            }
            params = new MethodParam(number, false);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BlockJson {

        @JsonProperty("number")
        private String blockNumber;

        @JsonProperty("hash")
        private String hash;

        @JsonProperty("parentHash")
        private String parentHash;

        @JsonProperty("logsBloom")
        private String logsBloom;

        @JsonProperty("transactionsRoot")
        private String transactionsRoot;

        @JsonProperty("stateRoot")
        private String stateRoot;

        @JsonProperty("receiptsRoot")
        private String receiptsRoot;

        @JsonProperty("difficulty")
        private String difficulty;

        @JsonProperty("totalDifficulty")
        private String totalDifficulty;

        @JsonProperty("miner")
        private String miner;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("nonce")
        private String nonce;

        @JsonProperty("solution")
        private String solution;

        @JsonProperty("gasUsed")
        private String gasUsed;

        @JsonProperty("gasLimit")
        private String gasLimit;

        @JsonProperty("nrgUsed")
        private String energyConsumed;

        @JsonProperty("nrgLimit")
        private String energyLimit;

        @JsonProperty("extraData")
        private String extraData;

        @JsonProperty("size")
        private String size;

        @JsonProperty("transactions")
        private List<String> transactionHashes;

        @JsonIgnore
        public String getHash() {
            return hash;
        }

        @JsonIgnore
        public NumericalValue getBlockNumber() {
            return new NumericalValue(blockNumber);
        }

        @JsonIgnore
        public String getParentHash() {
            return parentHash;
        }

        @JsonIgnore
        public String getLogsBloom() {
            return logsBloom;
        }

        @JsonIgnore
        public String getTransactionsRoot() {
            return transactionsRoot;
        }

        @JsonIgnore
        public String getStateRoot() {
            return stateRoot;
        }

        @JsonIgnore
        public String getReceiptsRoot() {
            return receiptsRoot;
        }

        @JsonIgnore
        public NumericalValue getDifficulty() {
            return new NumericalValue(difficulty);
        }

        @JsonIgnore
        public NumericalValue getTotalDifficulty() {
            return new NumericalValue(totalDifficulty);
        }

        @JsonIgnore
        public String getMiner() {
            return miner;
        }

        @JsonIgnore
        public NumericalValue getSize() {
            return new NumericalValue(size);
        }

        @JsonIgnore
        public List<String> getTransactionHashes() {
            return transactionHashes;
        }

        @JsonIgnore
        public NumericalValue getTimestamp() {
            return new NumericalValue(timestamp);
        }

        @JsonIgnore
        public String getNonce() {
            return nonce;
        }

        @JsonIgnore
        public String getSolution() {
            return solution;
        }

        @JsonIgnore
        public NumericalValue getGasUsed() {
            return new NumericalValue(gasUsed);
        }

        @JsonIgnore
        public NumericalValue getGasLimit() {
            return new NumericalValue(gasLimit);
        }

        @JsonIgnore
        public NumericalValue getEnergyConsumed() {
            return new NumericalValue(energyConsumed);
        }

        @JsonIgnore
        public NumericalValue getEnergyLimit() {
            return new NumericalValue(energyLimit);
        }

        @JsonIgnore
        public String getExtraData() {
            return extraData;
        }

        @Override
        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                System.out.println(e);
                return "failed to decode block json object";
            }
        }
    }

    public static class Response extends JsonRpcResponse {
        @JsonProperty("result")
        private BlockJson result;

        public BlockJson getResult() {
            return this.result;
        }
    }

    private static class MethodParam {
        @JsonProperty
        private String number;
        @JsonProperty
        private boolean retrieveTransactions;

        private MethodParam(String number, boolean retrieveTransactions) {
            this.retrieveTransactions = retrieveTransactions;
            this.number = number;

        }
    }
}
