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

package org.aion.bridge.chain.log;

import okhttp3.*;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class LoggingSetup {

    public static final Marker SMTP_MARKER = MarkerFactory.getMarker("SMTP_TRIGGER");

    private static final int POOL_MAX_IDLE_CONNECTIONS = 5;
    private static final long POOL_KEEP_ALIVE_DURATION = 5;
    private static final TimeUnit POOL_KEEP_ALIVE_DURATION_TIMEUNIT = TimeUnit.MINUTES;

    private static final long TIMEOUT_CONNECT = 10;
    private static final TimeUnit TIMEOUT_CONNECT_TIMEUNIT = TimeUnit.SECONDS;

    private static final long TIMEOUT_READ = 10;
    private static final TimeUnit TIMEOUT_READ_TIMEUNIT = TimeUnit.SECONDS;

    private static final long TIMEOUT_WRITE = 10;
    private static final TimeUnit TIMEOUT_WRITE_TIMEUNIT = TimeUnit.SECONDS;

    private static OkHttpClient client;

    private static String externalIp;

    public static String getExternalIpAddress() {

        if(externalIp == null) {
            ConnectionPool connectionPool = new ConnectionPool(
                    POOL_MAX_IDLE_CONNECTIONS, POOL_KEEP_ALIVE_DURATION, POOL_KEEP_ALIVE_DURATION_TIMEUNIT);

            // if shouldRetry is causing a problem, fix it here.
            // https://medium.com/inloopx/okhttp-is-quietly-retrying-requests-is-your-api-ready-19489ef35ace
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectionPool(connectionPool);
            builder.connectTimeout(TIMEOUT_CONNECT, TIMEOUT_CONNECT_TIMEUNIT);
            builder.readTimeout(TIMEOUT_READ, TIMEOUT_READ_TIMEUNIT);
            builder.writeTimeout(TIMEOUT_WRITE, TIMEOUT_WRITE_TIMEUNIT);

            client = builder.build();

            String url = "http://checkip.amazonaws.com";

            Call httpCall = null;
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                httpCall = client.newCall(request);
                Response response = httpCall.execute();

                if (response.code() != 200)
                    throw new RuntimeException("Unable to setup logging");

                externalIp = response.body().string();

                return externalIp;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return externalIp;
    }

    public static void setupLogging() {
        String externalIp = getExternalIpAddress();
        MDC.put("externalIp", externalIp);
        MDC.put("machineName", System.getProperty("user.name"));
        try {
            MDC.put("hostName", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            MDC.put("hostName", "null");
        }
    }
}