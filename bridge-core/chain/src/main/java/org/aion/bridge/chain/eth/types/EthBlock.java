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

package org.aion.bridge.chain.eth.types;

import org.aion.bridge.chain.base.types.Block;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

@Immutable
public class EthBlock extends Block {
    private final Word32 parentHash;
    private final BigInteger totalDifficulty;
    private final KeccakBloom logsBloom;
    private final long timestamp;
    private final List<Word32> transactionHashes;
    private final Word32 bytesEncoded;
    private final int hashCode;

    private final Logger log = LoggerFactory.getLogger(EthBlock.class.getName());


    public EthBlock(
            @Nonnull final Long number,
            @Nonnull final Word32 hash,
            @Nonnull final Word32 parentHash,
            @Nonnull final KeccakBloom logsBloom,
            @Nonnull final BigInteger totalDifficulty,
            final Long timestamp,
            @Nonnull final List<Word32> transactionHashes) {
        super(number, hash);

        log.trace("Number: {}", number);
        log.trace("Hash: {}", hash);
        log.trace("Parent: {}",parentHash);
        log.trace("Logs: {}",logsBloom);
        log.trace("TD: {}",totalDifficulty);
        log.trace("Timestamp: {}",timestamp);
        log.trace("Tx: {}",transactionHashes);

        this.parentHash = parentHash;
        this.logsBloom = logsBloom;
        this.timestamp = timestamp;
        this.totalDifficulty = totalDifficulty;
        this.transactionHashes = transactionHashes;
        this.bytesEncoded = computeByteEncoded();
        this.hashCode = Arrays.hashCode(this.bytesEncoded.payload());
    }

    @Override
    public KeccakBloom getLogsBloom() { return logsBloom; }

    @Override
    public long getTimestamp() { return timestamp; }

    @Override
    public List<Word32> getTransactionHashes() { return transactionHashes; }

    @Override
    public Word32 getParentHash() { return parentHash; }

    @Override
    public BigInteger getTotalDifficulty() { return totalDifficulty; }

    @Override
    public Word32 getBytesEncoded() { return bytesEncoded; }

    @Override
    public String toString() {

        return  "Block hash : " + this.hash.toString() + "\n" +
                "Parent hash: " + this.parentHash.toString() + "\n" +
                "LogsBloom  : " + this.getLogsBloom() + "\n" +
                "Number     : " + this.number + "\n" +
                "Timestamp  : " + this.getTimestamp() + "\n" +
                "Total Diff : " + this.totalDifficulty + "\n";
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EthBlock))
            return false;

        EthBlock otherBlock = (EthBlock) other;

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

    private Word32 computeByteEncoded() {

        final int BYTE_ENCODED_LENGTH = 32 + 8 + 32 + totalDifficulty.toByteArray().length + 256 + 8 + transactionHashes.size() * 32;

        ByteBuffer buf = ByteBuffer.allocate(BYTE_ENCODED_LENGTH);
        buf.put(hash.payload()); //32
        buf.putLong(number); //8
        buf.put(parentHash.payload()); //32
        buf.put(totalDifficulty.toByteArray()); //??
        buf.put(logsBloom.payload); //256
        buf.putLong(timestamp); //8
        for (Word32 transactionHash : transactionHashes) buf.put(transactionHash.payload());
        return new Word32(CryptoUtils.blake2b256(buf.array()));
    }

    private int computeHashCode(){
        return Arrays.hashCode(this.computeByteEncoded().payload());
    }
}
