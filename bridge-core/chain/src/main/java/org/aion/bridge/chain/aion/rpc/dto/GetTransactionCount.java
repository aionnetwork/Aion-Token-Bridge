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
import org.aion.bridge.chain.base.rpc.JsonRpcRequest;
import org.aion.bridge.chain.base.rpc.JsonRpcResponse;
import org.aion.bridge.chain.aion.rpc.NumericalValue;
import org.aion.bridge.chain.base.types.Word32;

import javax.annotation.Nonnull;

public class GetTransactionCount {
    public static class Request extends JsonRpcRequest {

        @JsonProperty("params")
        private String[] params;

        public Request(@Nonnull final Word32 address, @Nonnull final Object value) {
            super("eth_getTransactionCount");

            this.params = new String[2];
            this.params[0] = address.toStringWithPrefix();
            // TODO: this is lazy
            this.params[1] = value instanceof String ? "latest" : ((NumericalValue) value).toHexString();
        }
    }

    public static class Response extends JsonRpcResponse {
        @JsonProperty("result")
        private String result;

        public String getResult() {
            return this.result;
        }

        @JsonIgnore
        public NumericalValue getNumericalValue() {
            return new NumericalValue(result);
        }
    }
}
