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

package org.aion.monitor.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpProvider {


    private ExecutorService threadPoolExecutor = Executors.newCachedThreadPool();
    private ConnectionPool clientPool;
    private final String endpoint;

    // used to increment
    private final AtomicInteger counter = new AtomicInteger();
    private final ObjectMapper mapper = new ObjectMapper();

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf8");

    public HttpProvider(@Nonnull final String endpoint) {
        this.clientPool = new ConnectionPool(5, 300, TimeUnit.MILLISECONDS);
        this.endpoint = endpoint;
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture send(@Nonnull Map<String, String> params, @Nonnull final Class respClass) {
        return CompletableFuture.supplyAsync(() -> {

            OkHttpClient client = new OkHttpClient();

            HttpUrl.Builder builder = HttpUrl.parse(this.endpoint).newBuilder();
            if (params != null) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    builder.addQueryParameter(param.getKey(), param.getValue());
                }
            }

            Request request = new Request.Builder()
                    .url(builder.build())
                    .build();
            Response response = null;
            try {
                response = client.newCall(request).execute();

                if (response.code() != 200) {
                    throw new RuntimeException(String.format("HTTP invalid response: %d", response.code()));
                }

                ResponseBody body = response.body();
                if (body == null)
                    throw new RuntimeException("response is empty");

                if (respClass.equals(String.class)) {
                    return body.string();
                }
                return mapper.readValue(body.string(), respClass);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CompletionException(e);
            } finally {
                if (response != null)
                    response.close();

                // evict all active connections, this reduce performance but
                // may solve some issues related to server side
                if (client != null) {
                    client.connectionPool().evictAll();
                }
            }

        }, threadPoolExecutor);
    }

    public void shutdown() {
        threadPoolExecutor.shutdown();
    }
}


