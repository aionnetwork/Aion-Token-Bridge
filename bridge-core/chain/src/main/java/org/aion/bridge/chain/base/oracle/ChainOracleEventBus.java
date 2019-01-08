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

import org.aion.bridge.chain.base.types.Block;
import org.aion.bridge.chain.base.types.Log;
import org.aion.bridge.chain.base.types.Receipt;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Very simple event bus setup, this is designed to be used by {@link ChainOracle} to emit events back to
 * a caller on some event occurring. The possible events are currently defined by {@link Hook}
 *
 * Implementation Contract Part A:
 *
 * The implementation depends on a private data structure of Object types, which are cast to the appropriate
 * functional interface when execute(...) is called. Implicit in the implementation of this class is that direct
 * calls to execute(...) and register(...) are prohibited by users of this class.
 *
 * Instead, all calls to these private functions are intermediated by pairs of register/execute functions
 * that enforce correct types for each Hook.
 *
 * Implementation Contract Part B:
 *
 * How thread safety is guaranteed: all public functions (the register/execute pairs
 * described above) which comprise the API of this class must be protected by the intrinsic lock of this object
 * (by means of the synchronized keyword).
 *
 * Responsibility of the maintainer of this class to uphold these invariants to avoid running into RuntimeExceptions.
 */
@SuppressWarnings("unused")
@ThreadSafe
public class ChainOracleEventBus<B extends Block, R extends Receipt<L>, L extends Log> {

    @GuardedBy("this")
    private Map<Hook, List<Object>> eventMap = new HashMap<>();

    @GuardedBy("this")
    private void register(final Hook id, @Nonnull final Object func) {
        if (!this.eventMap.containsKey(id)) {
            eventMap.put(id, new ArrayList<>());
        }
        eventMap.get(id).add(func);
    }

    @GuardedBy("this")
    private void execute(@Nonnull Hook id) {
        List<Object> l = eventMap.get(id);
        if (l == null)
            return;

        l.forEach(f -> ((Runnable) f).run());
    }

    @SuppressWarnings("unchecked") // see class-level java-doc
    @GuardedBy("this")
    private <T> void execute(@Nonnull Hook id, T obj) {
        List<Object> l = eventMap.get(id);
        if (l == null)
            return;

        l.forEach(f -> ((Consumer<T>) f).accept(obj));
    }

    @SuppressWarnings("unchecked") // see class-level java-doc
    @GuardedBy("this")
    private <T, V> void execute(@Nonnull final Hook id, T obj1, V obj2) {
        List<Object> l = eventMap.get(id);
        if (l == null)
            return;

        l.forEach(f -> ((BiConsumer<T, V>) f).accept(obj1, obj2));
    }

    // Register Functions
    // ------------------
    // called by external actors to register events

    public synchronized void registerEnterHalt(Runnable func) { register(Hook.ENTER_HALT, func); }
    public synchronized void registerExitHalt(Runnable func) { register(Hook.EXIT_HALT, func); }

    public synchronized void registerEnterRecovery(Runnable func) { register(Hook.ENTER_RECOVERY, func); }
    public synchronized void registerExitRecovery(Runnable func) { register(Hook.EXIT_RECOVERY, func); }

    public synchronized void registerEnterPullBlocks(Runnable func) { register(Hook.ENTER_PULL_BLOCKS, func); }
    public synchronized void registerExitPullBlocks(Consumer<List<B>> func) { register(Hook.EXIT_PULL_BLOCKS, func); }

    public synchronized void registerEnterPullReceipt(Runnable func) { register(Hook.ENTER_PULL_RECEIPT, func); }
    public synchronized void registerExitPullReceipt(Consumer<List<R>> func) { register(Hook.EXIT_PULL_RECEIPT, func); }

    public synchronized void registerPublish(Consumer<ChainOracleResultset<B, R, L>> func) { register(Hook.PUBLISH, func); }

    public synchronized void registerShutdown(Runnable func) { register(Hook.SHUTDOWN, func); }

    // Execute Functions
    // -----------------
    // called by oracle scheduler thread to emit events executed synchronously

    public synchronized void executeEnterHalt() { execute(Hook.ENTER_HALT); }
    public synchronized void executeExitHalt() { execute(Hook.EXIT_HALT); }

    public synchronized void executeEnterRecovery() { execute(Hook.ENTER_RECOVERY); }
    public synchronized void executeExitRecovery() { execute(Hook.EXIT_RECOVERY); }

    public synchronized void executeEnterPullBlocks() { execute(Hook.ENTER_PULL_BLOCKS); }
    public synchronized void executeExitPullBlocks(List<B> pulledBlocks) { execute(Hook.EXIT_PULL_BLOCKS, pulledBlocks); }

    public synchronized void executeEnterPullReceiptCompleted() { execute(Hook.ENTER_PULL_RECEIPT); }
    public synchronized void executeExitPullReceiptCompleted(List<R> pulledReceipts) { execute(Hook.EXIT_PULL_RECEIPT, pulledReceipts); }

    public synchronized void executePublish(ChainOracleResultset<B, R, L> rs) { execute(Hook.PUBLISH, rs); }

    public synchronized void executeShutdown() { execute(Hook.SHUTDOWN); }

    /**
     * "Event hooks", that serve as keys for bucketing "functions" in this data structure **
     * ** by "functions", we mean "objects implementing functional interfaces"
     */
    private enum Hook {
        ENTER_HALT,
        EXIT_HALT,

        ENTER_RECOVERY,
        EXIT_RECOVERY,

        ENTER_PULL_BLOCKS,
        EXIT_PULL_BLOCKS,

        ENTER_PULL_RECEIPT,
        EXIT_PULL_RECEIPT,

        PUBLISH,

        SHUTDOWN
    }
}






















































