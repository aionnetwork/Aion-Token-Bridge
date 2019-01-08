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

package org.aion.bridge.chain.base;

import org.aion.bridge.chain.base.api.ApiFunction;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.types.Address;
import org.aion.bridge.chain.base.types.Block;
import org.aion.bridge.chain.base.types.Log;
import org.aion.bridge.chain.base.types.Receipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BlockNumberCollector <B extends Block, R extends Receipt<L>, L extends Log, A extends Address>{

    private final Logger log = LoggerFactory.getLogger(BlockNumberCollector.class);
    private ApiFunction<StatelessChainConnection, Long> method = c -> (Long) c.getBlockNumber();
    private int MAX_ACCEPTED_RANGE = 8;

    private Long latestBlockNumber = 0L;

    private List<StatelessChainConnection> connections;
    private final Executor executor;
    private final int quorum;
    private final long timeout;

    private final static TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final static long DEFAULT_TIMEOUT = 5_000L; // 3s

    public BlockNumberCollector(List<StatelessChainConnection<B, R, L, A>> connections, int quorum, ThreadPoolExecutor executor) {
        this(connections, quorum, Duration.ofMillis(DEFAULT_TIMEOUT), executor);
    }

    public BlockNumberCollector(List<StatelessChainConnection<B, R, L, A>> connections, int quorum, Duration timeout, Executor executor) {
        Objects.requireNonNull(connections);
        Objects.requireNonNull(timeout);
        if (connections.size() < 1) throw new IllegalArgumentException("connections.size() < 1");
        if (quorum < 1) throw new IllegalArgumentException("quorum < 1");
        if (timeout.isNegative()) throw new IllegalArgumentException("timeout < 0");
        if (timeout.isZero()) {
            log.warn("BlockNumberCollector: disabling timeout be setting timeout = 0");
        }

        this.timeout = timeout.toMillis();
        this.quorum = quorum;
        this.connections = Collections.unmodifiableList(connections);
        this.executor = executor;
    }

    // Error case: if the difference between the highest number and the lowest returned is higher than the max (8)
    // Update to the new block if the minimum in the range is higher than the last block recorded
    public Optional<Long> getLatestBlockNumber() throws QuorumNotAvailableException, InterruptedException {

        List<Long> blockNumberResult = Collector.batchCall(method, connections, executor, quorum, timeout, TIMEOUT_UNIT);
        Collections.sort(blockNumberResult);

        Long min = null;
        for (int i = 0; i <= blockNumberResult.size() - quorum; i++) {
            List<Long> subResultList = blockNumberResult.subList(i, i + quorum);

            if (subResultList.get(subResultList.size() - 1) - subResultList.get(0) > MAX_ACCEPTED_RANGE) {
                log.warn("Range in block numbers exceeds {}", MAX_ACCEPTED_RANGE);
                continue;
            }

            min = subResultList.get(0);
            break;
        }

        if (min != null && latestBlockNumber < min) {
            log.debug("Found new latestBlockNumber: {}", min);
            latestBlockNumber = min;
        }

        return Optional.ofNullable(latestBlockNumber);
    }
}
