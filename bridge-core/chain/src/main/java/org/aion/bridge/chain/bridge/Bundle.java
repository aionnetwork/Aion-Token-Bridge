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

import org.aion.bridge.chain.base.types.HashedState;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Immutable
public class Bundle implements HashedState, Comparable<Bundle> {

    private long ethBlockNumber;
    private Word32 ethBlockHash;
    private int indexInEthBlock;
    private Word32 bundleHash;
    private List<Transfer> transfers;
    private Word32 hashedState;
    private int hashCode;

    public Bundle(
            long ethBlockNumber,
            Word32 ethBlockHash,
            int indexInEthBlock,
            List<Transfer> transfers) {

        this.ethBlockNumber = ethBlockNumber;
        this.ethBlockHash = ethBlockHash;
        this.indexInEthBlock = indexInEthBlock;
        this.transfers = unmodifiableList(transfers);
        this.bundleHash = computeBundleHash(ethBlockHash, this.transfers);
        this.hashedState = computeHashedState();
        this.hashCode = Arrays.hashCode(this.hashedState.payload());
    }

    public long getEthBlockNumber() { return ethBlockNumber; }
    public Word32 getEthBlockHash() { return ethBlockHash; }
    public int getIndexInEthBlock() { return indexInEthBlock; }
    public List<Transfer> getTransfers() { return transfers; }
    public Word32 getBundleHash() { return bundleHash; }

    /**
     * @implNote Needs to implement the same order of concatenating the data as defined in
     * modPrecompiled/.../contracts/ATB/BridgeUtilities.java
     */
    private static Word32 computeBundleHash(Word32 ethBlockHash, List<Transfer> transfers) {
        int size = Word32.LENGTH + transfers.size() * Transfer.BYTE_ENCODED_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.put(ethBlockHash.payload());
        for (Transfer transfer : transfers) {
            buf.put(transfer.getTransferInBytes().getByteArray());
        }
        return new Word32(CryptoUtils.blake2b256(buf.array()));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Bundle))
            return false;

        Bundle other = (Bundle) o;

        if (this.transfers.size() != other.transfers.size())
            return false;

        for (int i = 0; i < transfers.size(); i++) {
            if (!transfers.get(i).equals(other.getTransfers().get(i)))
                return false;
        }

        return (other.ethBlockNumber == this.ethBlockNumber &&
                other.indexInEthBlock == this.indexInEthBlock &&
                other.ethBlockHash.equals(this.ethBlockHash));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private Word32 computeHashedState(){
        final int BYTE_ENCODED_LENGTH = 8 + 32 + 4 + 32 + transfers.size() * 32;

        ByteBuffer buf = ByteBuffer.allocate(BYTE_ENCODED_LENGTH);
        buf.putLong(ethBlockNumber);
        buf.put(ethBlockHash.payload());
        buf.putInt(indexInEthBlock);
        buf.put(bundleHash.payload());
        for (Transfer transfer : transfers) buf.put(transfer.getHashedState().payload());
        return new Word32(CryptoUtils.blake2b256(buf.array()));
    }

    @Override
    public Word32 getHashedState() {
        return hashedState;
    }

    @Override
    public int compareTo(@Nonnull Bundle o) {
        int ethBlockCompare = Long.compare(ethBlockNumber, o.ethBlockNumber);
        if (ethBlockCompare != 0) return ethBlockCompare;
        return Long.compare(indexInEthBlock, o.indexInEthBlock);
    }
}
