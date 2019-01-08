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

package org.aion.bridge.nexus;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContextBuilder;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.Signature;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.aion.bridge.grpc.signatory.SignatoryServiceGrpc;
import org.aion.bridge.grpc.signatory.SignedBundle;
import org.aion.bridge.grpc.signatory.ValidateAndSignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SignatoryGrpcConnection implements SignatoryConnection {
    private enum TLS {
        NONE, SERVER_ONLY, MUTUAL
    }

    public static class Builder {
        private String publicKey;
        private String host;
        private int port;
        private boolean tlsEnabled = false;
        private String clientCertChainFilePath;
        private String clientPrivateKeyFilePath;
        private String trustCertCollectionFilePath;

        public Builder publicKey(String x) { publicKey = x; return this; }
        public Builder host(String x) { host = x; return this; }
        public Builder port(int x) { port = x; return this; }
        public Builder tlsEnabled(boolean x) { tlsEnabled = x; return this; }
        public Builder clientCertChainFilePath(String x) { clientCertChainFilePath = x; return this; }
        public Builder clientPrivateKeyFilePath(String x) { clientPrivateKeyFilePath = x; return this; }
        public Builder trustCertCollectionFilePath(String x) { trustCertCollectionFilePath = x; return this; }

        public SignatoryGrpcConnection build() throws IOException {
            if (tlsEnabled) {
                Objects.requireNonNull(trustCertCollectionFilePath);
            }

            Objects.requireNonNull(publicKey);
            Objects.requireNonNull(host);
            if (port < 1 || port > 65535) throw new RuntimeException("port < 1 || port > 65535");

            return new SignatoryGrpcConnection(this);
        }
    }

    private final Word32 publicKey;
    private final ManagedChannel channel;
    private final TLS tls;
    private final SignatoryServiceGrpc.SignatoryServiceBlockingStub stub;

    public SignatoryGrpcConnection(Builder builder) throws IOException {
        publicKey = new Word32(builder.publicKey);

        NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(builder.host, builder.port)
                .keepAliveWithoutCalls(true)
                .keepAliveTime(3, TimeUnit.MINUTES);

        if (!builder.tlsEnabled) {
            channel = channelBuilder.usePlaintext().build();
            stub = SignatoryServiceGrpc.newBlockingStub(channel);
            tls = TLS.NONE;
            return;
        }

        // ssl
        SslContextBuilder ssl = GrpcSslContexts.forClient();
        ssl.trustManager(new File(builder.trustCertCollectionFilePath));

        if (builder.clientCertChainFilePath != null && builder.clientPrivateKeyFilePath != null) {
            ssl.keyManager(new File(builder.clientCertChainFilePath), new File(builder.clientPrivateKeyFilePath));
            tls = TLS.MUTUAL;
        } else {
            tls = TLS.SERVER_ONLY;
        }

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.err.println("TLS: " + tls.name());
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");


        channel = channelBuilder.negotiationType(NegotiationType.TLS).sslContext(ssl.build()).build();
        stub = SignatoryServiceGrpc.newBlockingStub(channel);
    }


    // exception thrown for any cause in signatory will be caught in collector
    public Signature getSignatureForBundle(StatefulBundle bundle) throws StatusRuntimeException {

        ValidateAndSignRequest request = ValidateAndSignRequest.newBuilder()
                .setSourceChainBlockNumber(bundle.getEthBlockNumber())
                .setSourceChainBlockHash(ByteString.copyFrom(bundle.getEthBlockHash().payload()))
                .setIndexInSourceChainBlock(bundle.getIndexInEthBlock())
                .setBundleHash(ByteString.copyFrom(bundle.getBundleHash().payload()))
                .build();

        SignedBundle response = stub.validateAndSign(request);

        return new Signature(new ImmutableBytes(response.getSignature().toByteArray()), publicKey);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
