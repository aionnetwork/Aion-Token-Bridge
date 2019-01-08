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

package org.aion.bridge.chain.base.types;

import org.aion.bridge.chain.base.utility.CryptoUtils;

import javax.annotation.concurrent.Immutable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Immutable
public class BlockWithReceipts<B extends Block, R extends Receipt<L>, L extends Log> {
    private final B block;
    private final List<R> receipts;
    private int hashCode;

    public BlockWithReceipts(B block, List<R> receipts) {
        Objects.requireNonNull(block);
        Objects.requireNonNull(receipts);

        this.block = block;
        this.receipts = Collections.unmodifiableList(receipts);
        this.hashCode = Arrays.hashCode(this.computeByteEncoded().payload());
    }

    public BlockWithReceipts(final B block) {
        Objects.requireNonNull(block);

        this.block = block;
        this.receipts = Collections.emptyList();
        this.hashCode = Arrays.hashCode(this.computeByteEncoded().payload());
    }

    public B getBlock() { return block; }
    public List<R> getReceipts() { return receipts; }

    public boolean isEmpty() {
        return receipts.isEmpty();
    }

    public int compareByBlockNumber(BlockWithReceipts other) {
        long b1Num = getBlock().getNumber();
        long b2Num = other.getBlock().getNumber();

        if (b1Num > b2Num) return 1;
        if (b1Num == b2Num) return 0;
        return -1;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BlockWithReceipts))
            return false;

        return ((BlockWithReceipts) other).block.equals(this.block) &&
                ((BlockWithReceipts) other).receipts.equals(this.receipts);

    }

    private Word32 computeByteEncoded() {

        final int BYTE_ENCODED_LENGTH = 32 + receipts.size() * 32;

        ByteBuffer buf = ByteBuffer.allocate(BYTE_ENCODED_LENGTH);
        buf.put(block.getBytesEncoded().payload());
        for (Receipt receipt : receipts) buf.put(receipt.getBytesEncoded().payload());
        return new Word32(CryptoUtils.blake2b256(buf.array()));
    }
}
