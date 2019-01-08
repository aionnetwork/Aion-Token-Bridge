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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.base.serialize.TransferDeserializer;
import org.aion.bridge.chain.base.serialize.TransferSerializer;
import org.aion.bridge.chain.base.types.HashedState;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word16;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.ByteUtils;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import javax.annotation.concurrent.Immutable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

@JsonSerialize(using = TransferSerializer.class)
@JsonDeserialize(using = TransferDeserializer.class)
@SuppressWarnings("WeakerAccess")
@Immutable
public class Transfer implements HashedState {

    public static final int BYTE_ENCODED_LENGTH = 32 + 32 + 16;

    @JsonProperty
    Word32 ethTxHash;

    @JsonProperty
    Word16 aionTransferAmount;

    @JsonProperty
    AionAddress aionAddress;

    @JsonIgnore
    Word32 hashedState;

    @JsonProperty
    ImmutableBytes transferInBytes;

    @JsonIgnore
    private int hashCode;

    public Transfer(Word32 ethTxHash, AionAddress aionAddress, Word16 aionTransferAmount) {
        this.ethTxHash = ethTxHash;
        this.aionTransferAmount = aionTransferAmount;
        this.aionAddress = aionAddress;
        this.transferInBytes = getBytes(ethTxHash, aionAddress, aionTransferAmount);
        this.hashedState = computeHashedState();
        this.hashCode = Arrays.hashCode(this.hashedState.payload());
    }

    public Transfer(Word32 ethTxHash, AionAddress aionAddress, BigInteger aionTransferAmount) {
        this (ethTxHash, aionAddress, new Word16(ByteUtils.pad(aionTransferAmount.toByteArray(), 16)));
    }

    public Word32 getEthTxHash() { return ethTxHash; }
    public Word16 getAionTransferAmount() { return aionTransferAmount; }
    public AionAddress getAionAddress() { return aionAddress; }

    @JsonIgnore
    public ImmutableBytes getTransferInBytes() { return transferInBytes; }
    @JsonIgnore
    public Word32 getHashedState() { return hashedState; }

    /**
     * @implNote Needs to implement the same order of concatenating the data as defined in
     * modPrecompiled/.../contracts/ATB/BridgeUtilities.java
     */
    private static ImmutableBytes getBytes(Word32 ethTxHash, AionAddress aionAddress, Word16 ethTokenAmount) {
        ByteBuffer buf = ByteBuffer.allocate(BYTE_ENCODED_LENGTH);
        buf.put(ethTxHash.payload());
        buf.put(aionAddress.payload());
        buf.put(ethTokenAmount.payload());
        return new ImmutableBytes(buf.array());
    }

    private Word32 computeHashedState() {
        return new Word32(CryptoUtils.blake2b256(this.transferInBytes.getByteArray()));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transfer))
            return false;

        Transfer other = (Transfer) o;
        return (this.ethTxHash.equals(other.ethTxHash) &&
                this.aionTransferAmount.equals(other.aionTransferAmount) &&
                this.aionAddress.equals(other.aionAddress));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
