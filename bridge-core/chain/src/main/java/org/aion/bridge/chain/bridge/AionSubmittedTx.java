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

import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.base.types.Word32;

public class AionSubmittedTx {
    private Long aionBlockNumber;
    private Word32 aionTxHash;
    private AionAddress from;
    private Long nonce;

    public AionSubmittedTx(AionAddress from, Long nonce, Long aionBlockNumber, Word32 aionTxHash) {
        this.aionBlockNumber = aionBlockNumber;
        this.aionTxHash = aionTxHash;
        this.from = from;
        this.nonce = nonce;
    }

    public Long getAionBlockNumber() {
        return aionBlockNumber;
    }

    public Word32 getAionTxHash() {
        return aionTxHash;
    }

    public AionAddress getFrom() {
        return from;
    }

    public Long getNonce() {
        return nonce;
    }

    public void setAionTxHash(Word32 aionTxHash) {
        this.aionTxHash = aionTxHash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block Number: ");
        sb.append(aionBlockNumber);
        sb.append("\n");
        sb.append("TxHash: ");
        sb.append(aionTxHash.toStringWithPrefix());
        sb.append("\n");
        sb.append("From: ");
        sb.append(from.toStringWithPrefix());
        sb.append("\n");
        sb.append("Nonce: ");
        sb.append(nonce);
        sb.append("\n");

        return sb.toString();
    }
}
