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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.aion.bridge.chain.base.rpc.JsonRpcRequest;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO: Not tested or used in any code-paths.
 */
@ThreadSafe
public class RestApiProvider {
    private ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();
    private final String endpoint;

    // used to increment
    private final AtomicInteger counter = new AtomicInteger();
    private final ObjectMapper mapper = new ObjectMapper();

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf8");

    public RestApiProvider(@Nonnull final String endpoint) {
        this.endpoint = endpoint;
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture send(@Nonnull final JsonRpcRequest call, @Nonnull final Class responseClass) {

        return CompletableFuture.supplyAsync(() -> {
            OkHttpClient client = null;
            Response resp = null;
            try {
                if (!mapper.canSerialize(call.getClass()))
                    throw new RuntimeException("cannot serializers call object");

                Request request = new Request.Builder()
                        .url(this.endpoint)
                        .post(RequestBody.create(MEDIA_TYPE_JSON, mapper.writeValueAsString(call)))
                        .build();

                // we want to retrieve this at the latest moment possible
                resp = client.newCall(request).execute();

                if (resp.code() != 200) {
                    throw new RuntimeException(String.format("HTTP invalid response: %d", resp.code()));
                }

                ResponseBody body = resp.body();
                if (body == null)
                    throw new RuntimeException("response is wrapWithEmptyBlock");

                if (responseClass.equals(String.class)) {
                    return body.string();
                }

                return mapper.readValue(body.string(), responseClass);
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                if (resp != null)
                    resp.close();


                if (client != null) {
                    /**
                     * evict all active connections; done to solve transient
                     * connection issues to kernel-side http-server
                     */
                    client.connectionPool().evictAll();
                }
            }
        }, threadPoolExecutor);
    }

    public void shutdown() {
        threadPoolExecutor.shutdown();
    }
}

