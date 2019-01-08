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

import com.google.common.collect.Sets;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.bridge.Signature;
import org.aion.bridge.chain.bridge.AionSubmittedTx;

import java.util.List;
import java.util.function.Predicate;

public class Predicates {

    public static final class ReceiptFailPredicate<T> implements Predicate<AionReceipt> {
        @Override
        public boolean test(AionReceipt receipt) {
            return receipt == null;
        }
    }

    public static final class SignatureFailPredicate<T> implements Predicate<List<Signature>> {
        @Override
        public boolean test(List<Signature> sigs) {
            return sigs == null || sigs.size() == 0 || Sets.newHashSet(sigs).size() != sigs.size() || sigs.contains(null);
            // done in Collector: sigs.size() < .minSignaturesRequired |in Signature: Signature byteSize
        }
    }

    public static final class TxInfoFailPredicate<T> implements Predicate<AionSubmittedTx> {
        @Override
        public boolean test(AionSubmittedTx aionSubmittedTx) {
            return aionSubmittedTx == null || aionSubmittedTx.getAionTxHash() == null || aionSubmittedTx.getAionBlockNumber() == null ||
                    aionSubmittedTx.getFrom() == null || aionSubmittedTx.getNonce() == null;

        }
    }
}
