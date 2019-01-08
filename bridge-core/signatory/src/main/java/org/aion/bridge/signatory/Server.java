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

package org.aion.bridge.signatory;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.aion.bridge.chain.log.LoggingSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Implements GRpc Server with Mutual Auth
 * See: https://github.com/grpc/grpc-java/tree/master/examples#hello-world-with-tls
 */
@SuppressWarnings("WeakerAccess")
public class Server {
    private enum TLS {
        NONE, SERVER_ONLY, MUTUAL
    }

    public static class Builder {
        private boolean tlsEnabled = false;
        private String host;
        private int port = 0;
        private String serverCertChainFilePath;
        private String serverPrivateKeyFilePath;
        private String trustCertCollectionFilePath;
        private Controller controller;

        public Builder tlsEnabled(boolean x) { tlsEnabled = x; return this; }
        public Builder host(String x) { host = x; return this; }
        public Builder port(int x) { port = x; return this; }
        public Builder serverCertChainFilePath(String x) { serverCertChainFilePath = x; return this; }
        public Builder serverPrivateKeyFilePath(String x) { serverPrivateKeyFilePath = x; return this; }
        public Builder trustCertCollectionFilePath(String x) { trustCertCollectionFilePath = x; return this; }
        public Builder controller(Controller x) { controller = x; return this; }

        public Server build() throws IOException {
            if (tlsEnabled) {
                Objects.requireNonNull(serverCertChainFilePath);
                Objects.requireNonNull(serverPrivateKeyFilePath);
            }

            Objects.requireNonNull(controller);
            Objects.requireNonNull(host);
            if (port < 1 || port > 65535) throw new RuntimeException("port < 1 || port > 65535");

            return new Server(this);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final io.grpc.Server server;
    private final TLS tls;
    private final String host;

    private Server(Builder builder) throws IOException {
        this.host = builder.host;

        LoggingSetup.setupLogging();

        NettyServerBuilder serverBuilder = NettyServerBuilder
                .forAddress(new InetSocketAddress(builder.host, builder.port))
                .permitKeepAliveWithoutCalls(true)
                .permitKeepAliveTime(2, TimeUnit.MINUTES)
                .addService(builder.controller);

        if (!builder.tlsEnabled) {
            server = serverBuilder.build();
            tls = TLS.NONE;
            return;
        }

        // ssl
        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(
                new File(builder.serverCertChainFilePath),
                new File(builder.serverPrivateKeyFilePath));

        if (builder.trustCertCollectionFilePath != null) {
            sslClientContextBuilder.trustManager(new File(builder.trustCertCollectionFilePath));
            sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
            tls = TLS.MUTUAL;
        } else {
            tls = TLS.SERVER_ONLY;
        }

        SslContext ssl = GrpcSslContexts.configure(sslClientContextBuilder, SslProvider.OPENSSL).build();

        server = serverBuilder.sslContext(ssl).build();
    }

    public void start() throws IOException {
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(Server.this::stop));
        log.info("Signatory server (tls: {}) started on [{}:{}]", tls.name(), host, server.getPort());
    }

    public void runUntilShutdown() throws InterruptedException {
        if (server != null)
            server.awaitTermination();
    }

    public void stop() {
        log.info("Signatory server stopping ...");
        if (server != null)
            server.shutdown();
        log.info("Signatory server stopped.");
    }
}
