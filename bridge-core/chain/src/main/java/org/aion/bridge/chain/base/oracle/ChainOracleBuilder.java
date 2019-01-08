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

package org.aion.bridge.chain.base.oracle;

import org.aion.bridge.chain.base.BlockNumberCollector;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.types.*;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.ObjectUtils.allNotNull;

public class ChainOracleBuilder<B extends Block, R extends Receipt<L>, L extends Log, A extends Address> {
    // Required parameters
    StatelessChainConnection<B, R, L, A> connection;
    BlockNumberCollector<B, R, L, A> blockNumberCollector;
    EventFilter filter;
    ChainHistory<B, R, L> history;

    // Optional parameters
    int tipDistance = 128;
    int blockBatchSize = 100;
    int receiptBatchSize = 500;
    Logger log = null;

    long haltDelay = 5;
    TimeUnit haltDelayTimeUnit = TimeUnit.SECONDS;

    public ChainOracleBuilder<B, R, L, A> setConnection(StatelessChainConnection<B, R, L, A> x) { connection = x; return this; }
    public ChainOracleBuilder<B, R, L, A> setFilter(EventFilter x) { filter = x; return this; }
    public ChainOracleBuilder<B, R, L, A> setTipDistance(int x) { tipDistance = x; return this; }
    public ChainOracleBuilder<B, R, L, A> setHaltDelay(TimeUnit x, long y) { haltDelayTimeUnit = x; haltDelay = y; return this; }
    public ChainOracleBuilder<B, R, L, A> setHistory(ChainHistory<B, R, L> x) { history = x; return this; }
    public ChainOracleBuilder<B, R, L, A> setLogger(Logger x) { log = x; return this; }
    public ChainOracleBuilder<B, R, L, A> setBlockCollector(BlockNumberCollector<B, R, L, A> x) { blockNumberCollector = x; return this; }

    public ChainOracleBuilder<B, R, L, A> setBlockBatchSize(Integer x) {
        if (x != null && x > 0) blockBatchSize = x;
        return this;
    }
    public ChainOracleBuilder<B, R, L, A> setReceiptBatchSize(Integer x) {
        if (x != null && x > 0) receiptBatchSize = x;
        return this;
    }

    public ChainOracle<B, R, L, A> build() {
        if (allNotNull(connection, filter, history, blockNumberCollector))
            return new ChainOracle<>(this);

        else throw new IllegalStateException();
    }
}