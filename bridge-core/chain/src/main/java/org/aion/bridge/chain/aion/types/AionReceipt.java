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

import org.aion.bridge.chain.base.types.Receipt;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class AionReceipt implements Receipt<AionLog> {
    private final Word32 transactionHash;
    private final Word32 blockHash;
    private final AionAddress from;
    private final AionAddress to;
    private final BlakeBloom logsBloom;
    private final List<AionLog> logs;
    private final Word32 bytesEncoded;
    private final int hashCode;
    private final boolean status;
    private final long blockNumber;
    private final int transactionIndex;

    public AionReceipt(@Nonnull final long blockNumber,
                       @Nonnull final Word32 transactionHash,
                       @Nonnull final Word32 blockHash,
                       @Nonnull final AionAddress from,
                       final AionAddress to,
                       @Nonnull final BlakeBloom logsBloom,
                       @Nonnull final List<AionLog> logs,
                       final boolean status,
                       final int transactionIndex) {
        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.blockHash = blockHash;
        this.from = from;
        this.to = to == null ? AionAddress.ZERO_ADDRESS : to;
        this.logsBloom = logsBloom;
        this.logs = logs;
        this.status = status;
        this.bytesEncoded = computeByteEncoded();
        this.transactionIndex = transactionIndex;
        this.hashCode = Arrays.hashCode(this.bytesEncoded.payload());
    }

    @Override public Word32 getTransactionHash() { return transactionHash; }
    @Override public Word32 getBlockHash() { return blockHash; }
    @Override public AionAddress getFrom() { return from; }
    @Override public AionAddress getTo() { return to; }
    @Override public BlakeBloom getLogsBloom() { return logsBloom; }
    @Override public List<AionLog> getEventLogs() { return logs; }
    @Override public boolean getStatus() { return status; }
    @Override public long getBlockNumber() { return blockNumber; }
    @Override public Word32 getBytesEncoded() { return bytesEncoded; }

    @Override
    public int getTransactionIndex() {
        return transactionIndex;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AionReceipt)) return false;
        if (this.blockNumber != ((AionReceipt) other).getBlockNumber()) return false;
        if (!this.transactionHash.equals(((AionReceipt) other).getTransactionHash())) return false;
        if (!this.blockHash.equals(((AionReceipt) other).getBlockHash())) return false;
        if (!this.from.equals(((AionReceipt) other).getFrom())) return false;
        if (!this.to.equals(((AionReceipt) other).getTo())) return false;
        if (this.status != (((AionReceipt) other).getStatus())) return false;
        if (!this.logsBloom.equals(((AionReceipt) other).getLogsBloom())) return false;
        if (!this.logs.equals(((AionReceipt) other).getEventLogs())) return false;
        if (this.transactionIndex != ((AionReceipt) other).transactionIndex) return false;

        return true;
    }

    @Override
    public int hashCode() { return hashCode; }

    private Word32 computeByteEncoded() {

        final int BYTE_ENCODED_LENGTH = 8 + 32 + 32 + 32 + 32 + 256 + logs.size() * 32 + 1 + 4;

        ByteBuffer buf = ByteBuffer.allocate(BYTE_ENCODED_LENGTH);
        buf.putLong(blockNumber);
        buf.put(transactionHash.payload());
        buf.put(blockHash.payload());
        buf.put(from.payload());
        buf.put(to.payload());
        buf.put(logsBloom.payload);
        for (AionLog log: logs) buf.put(log.getBytesEncoded().payload());
        buf.put((byte) (status ? 1:0));
        buf.putInt(transactionIndex);
        return new Word32(CryptoUtils.blake2b256(buf.array()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("transactionHash: ");
        sb.append(transactionHash);
        sb.append(", blockHash: ");
        sb.append(blockHash);
        sb.append(", from: ");
        sb.append(from);
        sb.append(", logsBloom: ");
        sb.append(logsBloom);
        sb.append(", bytesEncoded: ");
        sb.append(bytesEncoded);
        sb.append(", hashCode: ");
        sb.append(hashCode);
        sb.append(", status: ");
        sb.append(status);
        sb.append(", blockNumber: ");
        sb.append(blockNumber);
        for(AionLog l : logs) {
            sb.append(l.toString());
        }
        return sb.toString();
    }

    private int computeHashCode() {
        return Arrays.hashCode(this.computeByteEncoded().payload());
    }
}
