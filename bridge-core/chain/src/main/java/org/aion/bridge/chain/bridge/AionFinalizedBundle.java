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

package org.aion.bridge.chain.bridge;

import org.aion.bridge.chain.base.types.Word32;


public class AionFinalizedBundle implements Comparable<AionFinalizedBundle> {
    private long bundleId;
    private Word32 bundleHash;
    private Word32 aionTxHash;
    private long aionBlockNumber;
    private Word32 getAionTxHash;

    public AionFinalizedBundle(long bundleId, Word32 bundleHash, Word32 aionTxHash, long aionBlockNumber, Word32 getAionTxHash)

    {
        this.bundleId = bundleId;
        this.bundleHash = bundleHash;
        this.aionTxHash = aionTxHash;
        this.aionBlockNumber = aionBlockNumber;
        this.getAionTxHash = getAionTxHash;
    }

    public long getBundleId() {
        return bundleId;
    }

    public Word32 getBundleHash() {
        return bundleHash;
    }

    public Word32 getAionTxHash() {
        return aionTxHash;
    }

    public long getAionBlockNumber() {
        return aionBlockNumber;
    }

    public Word32 getGetAionTxHash() {
        return getAionTxHash;
    }

    @Override
    public int compareTo(AionFinalizedBundle o) {
        return Long.compare(bundleId, o.bundleId);
    }
}