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

import com.google.common.base.Stopwatch;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.aion.bridge.grpc.enclave.EnclaveServiceGrpc;
import org.aion.bridge.grpc.enclave.SignRequest;
import org.aion.bridge.grpc.enclave.SignedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @implNote Communication to Enclave is unsecured, since that service is going to be running on
 * the same machine as the signatory server.
 *
 * @implNote Ok to duplicate this code, since best available alternative breaks encapsulation model
 */
@SuppressWarnings({"WeakerAccess", "Duplicates"})
public class Enclave {

    private static final Logger log = LoggerFactory.getLogger(Enclave.class.getName());
    private final EnclaveServiceGrpc.EnclaveServiceBlockingStub stub;

    public Enclave(String url) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(url)
                .usePlaintext().build();

        stub = EnclaveServiceGrpc.newBlockingStub(channel);
        log.info("Connected to enclave server at {}", url);
    }

    // gRPC's opinion is that it's better for RPC-type interfaces to deal in Runtime exceptions
    public SignedResponse sign(ByteString payload) throws StatusRuntimeException {
        Stopwatch timer = null;
        if (log.isTraceEnabled()) timer = Stopwatch.createStarted();
        SignRequest request = SignRequest.newBuilder().setData(payload).build();
        SignedResponse response = stub.sign(request);
        log.trace("Received signature from enclave in [{}]ms.", timer != null ? timer.stop().elapsed(TimeUnit.MILLISECONDS) : -1);
        return response;
    }
}
