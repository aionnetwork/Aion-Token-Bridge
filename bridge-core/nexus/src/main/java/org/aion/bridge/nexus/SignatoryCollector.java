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

import io.grpc.StatusRuntimeException;
import org.aion.bridge.chain.base.Collector;
import org.aion.bridge.chain.base.api.ApiFunction;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.bridge.Signature;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SignatoryCollector {

    private List<SignatoryGrpcConnection> connections;
    private final Executor executor;

    private final int quorum;
    private final long timeout;

    private final static TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final static long DEFAULT_TIMEOUT = 10_000L; // 10s

    public SignatoryCollector(List<SignatoryGrpcConnection> connections, int quorum, ThreadPoolExecutor executor) {
        this(connections, quorum, Duration.ofMillis(DEFAULT_TIMEOUT), executor);
    }

    public SignatoryCollector(List<SignatoryGrpcConnection> connections, int quorum, Duration timeout, Executor executor) {
        Objects.requireNonNull(connections);
        Objects.requireNonNull(timeout);
        if (connections.size() < 1) throw new IllegalArgumentException("connections.size() < 1");
        if (quorum < 1) throw new IllegalArgumentException("quorum < 1");
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("timeout < 0");

        this.timeout = timeout.toMillis();
        this.quorum = quorum;
        this.connections = Collections.unmodifiableList(connections);
        this.executor = executor;
    }

    private <T> List<T> batchCall(ApiFunction<SignatoryGrpcConnection,T> method) throws InterruptedException, QuorumNotAvailableException {
        return Collector.batchCall(method, connections, executor, quorum, timeout, TIMEOUT_UNIT);
    }

    public List<Signature> getSignatureForBundle(StatefulBundle bundle) throws StatusRuntimeException, QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.getSignatureForBundle(bundle));
    }
}
