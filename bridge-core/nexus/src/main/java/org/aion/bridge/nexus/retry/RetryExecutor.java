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

package org.aion.bridge.nexus.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public final class RetryExecutor<R> {
    private final int maxAttempt;
    private final Predicate<R> rejectionPredicate;
    private int sleepTime;
    private final Logger log = LoggerFactory.getLogger(RetryExecutor.class);


    public RetryExecutor(@Nonnull int maxAttempt,
                         @Nonnull Predicate<R> rejectionPredicate,
                         @Nonnull int sleepTime) {

        this.maxAttempt = maxAttempt;
        this.rejectionPredicate = rejectionPredicate;
        this.sleepTime = sleepTime;
    }


    //will retry if exception is thrown
    public R execute(Callable<R> callable) {//throws RetryException {
        for (int attemptNumber = 1; attemptNumber <= maxAttempt; attemptNumber++) {
            try {
                R result = callable.call();
                if (!rejectionPredicate.test(result)) {
                    return result;
                }
            } catch (Throwable t) {
                // TODO
                log.debug("Error occurred during retry." + callable.getClass().getName());
                t.printStackTrace();
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }
}
