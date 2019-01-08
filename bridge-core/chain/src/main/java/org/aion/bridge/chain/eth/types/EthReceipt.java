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

import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Receipt;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * EthReceipt structure, a reduced version of receipts from Ethereum, as some
 * fields are considered irrelevant for the purposes of this application.
 * <p>
 * Exclusions are:
 * <li>
 * <ul>{@code gasConsumed}</ul>
 * <ul>{@code totalGasConsumed}</ul>
 * </li>
 */
public class EthReceipt implements Receipt<EthLog> {
    private final Word32 transactionHash;
    private final Word32 blockHash;
    private final EthAddress from;
    private final EthAddress to;
    private final KeccakBloom logsBloom;
    private final List<EthLog> logs;
    private final Word32 bytesEncoded;
    private final int hashCode;
    private final boolean status;
    private final long blockNumber;
    private final int transactionIndex;

    public EthReceipt(
            final long blockNumber,
            @Nonnull final Word32 transactionHash,
            @Nonnull final Word32 blockHash,
            @Nonnull final EthAddress from,
            final EthAddress to,
            @Nonnull final KeccakBloom logsBloom,
            @Nonnull final List<EthLog> logs,
            final boolean status,
            final int transactionIndex) {

        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.blockHash = blockHash;
        this.from = from;
        this.to = to == null ? EthAddress.ZERO_ADDRESS : to;
        this.logsBloom = logsBloom;
        this.logs = logs;
        this.status = status;
        this.transactionIndex = transactionIndex;
        this.bytesEncoded = computeByteEncoded();
        this.hashCode = Arrays.hashCode(this.computeByteEncoded().payload());
    }

    @Override
    public Word32 getTransactionHash() { return transactionHash; }

    @Override
    public Word32 getBlockHash() { return blockHash; }

    @Override
    public EthAddress getFrom() { return from; }

    @Override
    public EthAddress getTo() { return to; }

    @Override
    public KeccakBloom getLogsBloom() { return logsBloom; }

    @Override
    public List<EthLog> getEventLogs() { return logs; }

    @Override public boolean getStatus() { return status; }

    @Override
    public Word32 getBytesEncoded() { return bytesEncoded; }

    @Override
    public int getTransactionIndex() {
        return transactionIndex;
    }

    @Override
    public String toString() { return this.transactionHash.toString(); }

    @Override public long getBlockNumber() { return blockNumber; }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EthReceipt)) return false;
        if (this.blockNumber != ((EthReceipt) other).getBlockNumber()) return false;
        if (!this.transactionHash.equals(((EthReceipt) other).getTransactionHash())) return false;
        if (!this.blockHash.equals(((EthReceipt) other).getBlockHash())) return false;
        if (!this.from.equals(((EthReceipt) other).getFrom())) return false;
        if (!this.to.equals(((EthReceipt) other).getTo())) return false;
        if (this.status != (((EthReceipt) other).getStatus())) return false;
        if (!this.logsBloom.equals(((EthReceipt) other).getLogsBloom())) return false;
        //noinspection RedundantIfStatement
        if (!this.logs.equals(((EthReceipt) other).getEventLogs())) return false;
        if (this.transactionIndex != ((EthReceipt) other).transactionIndex) return false;

        return true;
    }

    @Override
    public int hashCode() { return hashCode; }

    private Word32 computeByteEncoded() {

        final int BYTE_ENCODED_LENGTH = 8 + 32 + 32 + 20 + 20 + 256 + logs.size() * 32 + 1 + 4;

        ByteBuffer buf = ByteBuffer.allocate(BYTE_ENCODED_LENGTH);
        buf.putLong(blockNumber);
        buf.put(transactionHash.payload());
        buf.put(blockHash.payload());
        buf.put(from.payload());
        buf.put(to.payload());
        buf.put(logsBloom.payload);
        for (EthLog log: logs) buf.put(log.getBytesEncoded().payload());
        buf.put((byte) (status ? 1:0));
        buf.putInt(transactionIndex);
        return new Word32(CryptoUtils.blake2b256(buf.array()));

    }
    private int computeHashCode() {
        return Arrays.hashCode(this.computeByteEncoded().payload());
    }

}
