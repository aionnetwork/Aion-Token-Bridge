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

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public class RetryBuilder<R> {
    private int maxAttempt;
    private Predicate<R> rejectionPredicate;
    private int sleepTime = 500;

    private RetryBuilder() {
    }

    public static <R> RetryBuilder<R> newBuilder() {
        return new RetryBuilder<>();
    }


    public RetryBuilder<R> stopAfterAttempt(@Nonnull int maxAttempt) throws IllegalStateException {
        this.maxAttempt = maxAttempt;
        return this;
    }

    public RetryBuilder<R> retryIf(@Nonnull Predicate<R> resultPredicate) {
        this.rejectionPredicate = resultPredicate;
        return this;
    }

    // in ms
    public RetryBuilder<R> setSleepTime(@Nonnull int sleepTime) {
        this.sleepTime = sleepTime;
        return this;
    }

    public RetryExecutor<R> build() {
        return new RetryExecutor<R>(maxAttempt, rejectionPredicate, sleepTime);
    }


}
