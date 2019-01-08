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

package org.aion.bridge.chain.base.api;

import org.aion.bridge.chain.base.Consolidator;
import org.aion.bridge.chain.base.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConsolidatedChainConnection<B extends Block, R extends Receipt<L>, L extends Log, A extends Address> implements StatelessChainConnection<B, R, L, A> {

    private final Logger log = LoggerFactory.getLogger(ConsolidatedChainConnection.class);
    private final List<StatelessChainConnection<B, R, L, A>> connections;
    private final Executor executor;

    private final int quorum;
    private final long timeout;

    private final static TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final static long DEFAULT_TIMEOUT = 10_000L; // 10s

    public ConsolidatedChainConnection(List<StatelessChainConnection<B, R, L, A>> connections, int quorum, ThreadPoolExecutor executor) {
        this(connections, quorum, Duration.ofMillis(DEFAULT_TIMEOUT), executor);
    }

    public ConsolidatedChainConnection(List<StatelessChainConnection<B, R, L, A>> connections, int quorum, Duration timeout, Executor executor) {
        Objects.requireNonNull(connections);
        Objects.requireNonNull(timeout);
        if (connections.size() < 1) throw new IllegalArgumentException("connections.size() < 1");
        if (quorum < 1) throw new IllegalArgumentException("quorum < 1");
        if (timeout.isNegative()) throw new IllegalArgumentException("timeout < 0");
        if (timeout.isZero()) {
            log.warn("ConsolidatedChainConnection: disabling timeout be setting timeout = 0");
        }

        this.timeout = timeout.toMillis();
        this.quorum = quorum;
        this.connections = Collections.unmodifiableList(connections);
        this.executor = executor;
    }

    private <T> T batchCall(ApiFunction<StatelessChainConnection<B, R, L, A>, T> method)
            throws QuorumNotAvailableException, InterruptedException {
        return Consolidator.batchCall(method, connections, executor, quorum, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Long getBlockNumber()
            throws QuorumNotAvailableException, InterruptedException {
        //noinspection Convert2MethodRef
        return batchCall(c -> getBlockNumber());
    }

    @Override
    public Optional<B> getBlock(long blockNumber)
            throws QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.getBlock(blockNumber));
    }

    @Override
    public Optional<R> getReceipt(Word32 transactionHash)
            throws QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.getReceipt(transactionHash));
    }

    @Override
    public List<B> getBlocksRangeClosed(long start, long end)
            throws QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.getBlocksRangeClosed(start, end));
    }

    @Override
    public List<BlockWithReceipts<B, R, L>> getReceiptsForBlocks(List<B> blocks)
            throws QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.getReceiptsForBlocks(blocks));
    }

    @Override
    public BigInteger getNonce(A address)
            throws QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.getNonce(address));
    }

    @Override
    public Word32 sendRawTransaction(ImmutableBytes rawTransaction)
            throws QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.sendRawTransaction(rawTransaction));
    }

    @Override
    public BigInteger getGasPrice()
            throws QuorumNotAvailableException, InterruptedException {
        //noinspection Convert2MethodRef
        return batchCall(c -> c.getGasPrice());
    }

    @Override
    public BigInteger getBalance(A address, String blockId)
            throws  QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.getBalance(address, blockId));
    }

    @Override
    public Integer peerCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String contractCall(A address, ImmutableBytes abi, String blockId)
            throws QuorumNotAvailableException, InterruptedException {
        return batchCall(c -> c.contractCall(address, abi, blockId));
    }

    @Override
    public String contractCall(A address, ImmutableBytes abi) throws QuorumNotAvailableException, InterruptedException {
        return contractCall(address, abi, "latest");
    }


    @Override
    public void evictConnections() {
       for(StatelessChainConnection c: connections){
           c.evictConnections();
       }
    }
}
