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

package org.aion.bridge.chain.aion.rpc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.aion.bridge.chain.aion.rpc.NumericalValue;
import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.base.rpc.JsonRpcRequest;
import org.aion.bridge.chain.base.rpc.JsonRpcResponse;

public class GetBalance {

    public static class Request extends JsonRpcRequest {
        @JsonProperty("params") public String[] params;

        public Request(AionAddress address) {
            this(address, "latest");
        }

        public Request(AionAddress address, String blockId) {
            super("eth_getBalance");
            this.params = new String[2];
            this.params[0] = address.toStringWithPrefix();
            this.params[1] = blockId;
        }
    }

    public static class AccountStateJson {
        @JsonProperty("address") private String address;
        @JsonProperty("balance") private String balance;
        @JsonProperty("nonce") private String nonce;
        @JsonProperty("blockNumber") private Long blockNumber;

        @JsonIgnore public AionAddress getAddress() { return new AionAddress(address); }
        @JsonIgnore public NumericalValue getBalance() { return new NumericalValue(balance); }
        @JsonIgnore public NumericalValue getNonce() { return new NumericalValue(nonce); }
        @JsonIgnore public Long getBlockNumber() { return blockNumber; }
    }

    public static class Response extends JsonRpcResponse {
        @JsonProperty("result")
        String result;

        public String getResult() { return result; }

        @JsonIgnore
        public NumericalValue getNumericalValue() {
            return new NumericalValue(result);
        }
    }
}
