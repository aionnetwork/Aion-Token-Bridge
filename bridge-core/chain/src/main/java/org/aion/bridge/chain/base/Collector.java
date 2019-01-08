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

import com.google.common.base.Stopwatch;
import org.aion.bridge.chain.base.api.ApiFunction;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class Collector {

    private static final Logger log = LoggerFactory.getLogger(Collector.class);

    public static <T, I> List<T> batchCall(ApiFunction<I, T> method, List<I> inputs, Executor executor, int quorum,
                                           long timeout, TimeUnit timeoutUnit) throws QuorumNotAvailableException, InterruptedException {
        return batchCall(method, inputs, executor, quorum, timeout, timeoutUnit, false);
    }
    /**
     * Collect responses and return the set of received responses up to the quorum.
     * Responses may be the same or different
     */
    public static <T, I> List<T> batchCall(ApiFunction<I, T> method, List<I> inputs, Executor executor, int quorum,
                                           long timeout, TimeUnit timeoutUnit, boolean returnAfterQuorumCollected)
            throws QuorumNotAvailableException, InterruptedException {

        CompletionService<T> cs = new ExecutorCompletionService<>(executor);
        Set<Future<T>> futures = new HashSet<>();

        //noinspection Duplicates
        for (I input : inputs) {
            futures.add(cs.submit(() -> {
                try {
                    return method.apply(input);
                } catch (Exception e) {
                    if (!(e.getCause() instanceof InterruptedIOException)) {
                        //e.printStackTrace();
                        log.debug("Collector encountered non-critical exception; Exception Message: {}", e.getMessage());
                    }
                }
                return null;
            }));
        }

        List<T> responses = new ArrayList<>();
        Stopwatch timer = Stopwatch.createStarted();

        Future<T> future;
        T i;

        boolean timeoutEnabled = true;
        if (timeout <= 0)
            timeoutEnabled = false;

        while (!Thread.currentThread().isInterrupted()) {
            if (futures.size() == 0) break;
            if (timeoutEnabled && timer.elapsed(timeoutUnit) > timeout) break;

            if (timeoutEnabled)
                future = cs.poll(timeout, timeoutUnit);
            else
                future = cs.take();

            if (future == null)  // exceeded timeout
                break;

            futures.remove(future);
            try {
                i = future.get();
                if (i != null) {
                    responses.add(i);
                }
            } catch (ExecutionException e) {
                log.debug("Collector encountered ExecutionException", e);
                e.printStackTrace();
                //noinspection UnnecessaryContinue
                continue; // ok so this response was bad, try the other ones
            }

            if (returnAfterQuorumCollected && responses.size() >= quorum) break;
        }

        // at this point, OK to cancel any outstanding futures
        for (Future<T> f : futures)
            f.cancel(true);

        if (responses.size() < quorum) {
            Thread.dumpStack();
            throw new QuorumNotAvailableException("Could not achieve quorum. total responses received: "
                    + responses.size() + " cancelled requests: " + futures.size() + " quorum: " + quorum);
        }

        return responses;
    }

}
