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
import java.util.*;
import java.util.concurrent.*;

public class Consolidator {
    private static final Logger log = LoggerFactory.getLogger(Consolidator.class);

    private static <T> T getConsolidatedResponse(Map<T, Integer> responses, int quorum) {
        for (T key : responses.keySet()) {
            if (responses.get(key) >= quorum) {
                return key;
            }
        }
        return null;
    }

    // Consolidates calls into a single response
    public static <T, I> T batchCall(ApiFunction<I, T> method, List<I> inputs, Executor executor,
                                     int quorum, long timeout, TimeUnit timeoutUnit)
            throws QuorumNotAvailableException, InterruptedException {
        CompletionService<T> cs = new ExecutorCompletionService<T>(executor);
        Set<Future<T>> futures = new HashSet<Future<T>>();

        //noinspection Duplicates
        for (I input : inputs) {
            futures.add(cs.submit(() -> {
                try {
                    return method.apply(input);
                } catch (Exception e) {
                    if (!(e.getCause() instanceof InterruptedIOException)) {
                        log.trace("Consolidator encountered non-critical exception; Exception Message: {}", e.getCause());
                    }
                }
                return null;
            }));
        }

        Map<T, Integer> responses = new HashMap<>();
        Stopwatch timer = Stopwatch.createStarted();

        T response = null;
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
                    if (responses.containsKey(i)) {
                        responses.put(i, responses.get(i) + 1);
                    } else {
                        responses.put(i, 1);
                    }
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
                continue; // ok so this response was bad, try the other ones
            }

            // now check if we've gotten a consolidated response. if not, keep looking
            response = getConsolidatedResponse(responses, quorum);
            if (response != null) break;
            else //noinspection UnnecessaryContinue
                continue;
        }

        // at this point, OK to cancel any outstanding futures
        for (Future<T> f : futures)
            f.cancel(true);

        if (response == null) {
            Thread.dumpStack();
            throw new QuorumNotAvailableException("Could not achieve quorum. total responses received: "
                    + responses.size() + " cancelled requests: " + futures.size() + " quorum: " + quorum);
        }

        return response;
    }
}
