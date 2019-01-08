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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.aion.bridge.chain.base.rpc.JsonRpcRequest;
import org.aion.bridge.chain.base.rpc.JsonRpcResponse;
import org.aion.bridge.chain.base.types.Address;
import org.aion.bridge.chain.base.types.ImmutableBytes;

import javax.annotation.Nonnull;

public class CallContract {

    private static class Payload {
        @JsonProperty("to")
        private String to;

        @JsonProperty("data")
        private String encodedData;

        public Payload(@Nonnull final String to,
                       @Nonnull final String data) {
            this.to = to;
            this.encodedData = data;
        }
    }

    public static class Request extends JsonRpcRequest {
        @JsonProperty("params")
        private Object[] params;

        public Request(Address address, ImmutableBytes abi) {
            this(address, abi, "latest");
        }

        public Request(Address address, ImmutableBytes abi, String blockId) {
            super("eth_call");
            params = new Object[2];
            params[0] = new Payload(address.toStringWithPrefix(), abi.toStringWithPrefix());
            params[1] = blockId;
        }
    }

    public static class Response extends JsonRpcResponse {
        @JsonProperty("result")
        private String result;

        public String getResult() {
            return result;
        }
    }
}