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

package org.aion.bridge.chain.aion.types;

import org.aion.bridge.chain.base.types.Block;
import org.aion.bridge.chain.base.types.Bloom;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class AionBlock extends Block {

    private final Word32 parentHash;
    private final BigInteger totalDifficulty;
    private final BlakeBloom logsBloom;
    private final long timestamp;
    private final List<Word32> transactionHashes;
    private final Word32 bytesEncoded;
    private final int hashCode;

    public AionBlock(@Nonnull final long number,
                     @Nonnull final Word32 hash,
                     @Nonnull final Word32 parentHash,
                     @Nonnull final BlakeBloom logsBloom,
                     @Nonnull final BigInteger totalDifficulty,
                     final long timestamp,
                     @Nonnull final List<Word32> transactionHashes) {
        super(number, hash);
        this.parentHash = parentHash;
        this.logsBloom = logsBloom;
        this.totalDifficulty = totalDifficulty;
        this.timestamp = timestamp;
        this.transactionHashes = transactionHashes;
        this.bytesEncoded = computeByteEncoded();
        this.hashCode = Arrays.hashCode(this.bytesEncoded.payload());
    }

    @Override public Bloom getLogsBloom() { return this.logsBloom; }
    @Override public long getTimestamp() { return this.timestamp; }
    @Override public List<Word32> getTransactionHashes() { return this.transactionHashes; }
    @Override public Word32 getParentHash() { return parentHash; }
    @Override public BigInteger getTotalDifficulty() { return totalDifficulty; }
    @Override public Word32 getBytesEncoded() { return bytesEncoded; }

    @Override
    public int hashCode() { return hashCode; }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AionBlock))
            return false;

        AionBlock otherBlock = (AionBlock) other;

        if (!this.hash.equals(otherBlock.hash))
            return false;
        if (!this.parentHash.equals(otherBlock.parentHash))
            return false;
        if (!this.logsBloom.equals(otherBlock.logsBloom))
            return false;
        if (this.number != otherBlock.number)
            return false;
        if (this.timestamp != otherBlock.timestamp)
            return false;
        if (!this.totalDifficulty.equals(otherBlock.totalDifficulty))
            return false;
        //noinspection RedundantIfStatement
        if (!(this.transactionHashes.equals(otherBlock.transactionHashes)))
            return false;

        return true;
    }
    @SuppressWarnings("Duplicates")
    private Word32 computeByteEncoded() {
        final int BYTE_ENCODED_LENGTH = 32 + 8 + 32 + totalDifficulty.toByteArray().length + 256 + 8 + transactionHashes.size() * 32;
        ByteBuffer buf = ByteBuffer.allocate(BYTE_ENCODED_LENGTH);

        buf.put(hash.payload());
        buf.putLong(number);
        buf.put(parentHash.payload());
        buf.put(totalDifficulty.toByteArray());
        buf.put(logsBloom.payload);
        buf.putLong(timestamp);
        for (Word32 transactionHash : transactionHashes) buf.put(transactionHash.payload());
        return new Word32(CryptoUtils.blake2b256(buf.array()));
    }

    private int computeHashCode() {
        return Arrays.hashCode(this.computeByteEncoded().payload());
    }
}
