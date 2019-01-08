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

import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import java.util.Arrays;

public class Signature {
    public static final int LENGTH = 96;

    private ImmutableBytes signature;
    private Word32 publicKey;
    private ImmutableBytes payload;
    private Word32 bytesEncoded;
    private int hashCode;

    public Signature(ImmutableBytes signature, Word32 publicKey) {
        if (signature.getLength() != 64)
            throw new IllegalArgumentException("signature must be 64 bytes.");

        this.publicKey = publicKey;
        this.signature = signature;
        this.payload = new ImmutableBytes(computePayload());
        this.bytesEncoded = computeByteEncoded();
        this.hashCode = Arrays.hashCode(this.computeByteEncoded().payload());
    }

    public ImmutableBytes getSignature() { return signature; }
    public Word32 getPublicKey() { return publicKey; }
    public byte[] payload() { return payload.getByteArray(); }

    private byte[] computePayload() {
        byte[] payload = new byte[LENGTH];
        System.arraycopy(signature.getByteArray(), 0, payload, 0, 64);
        System.arraycopy(publicKey.payload(), 0,payload, 64, 32);
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Signature))
            return false;
        Signature other = (Signature) o;

        return this.payload.equals(other.payload());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public Word32 getBytesEncoded() {
        return bytesEncoded;
    }

    private Word32 computeByteEncoded(){
        return new Word32(CryptoUtils.blake2b256(payload.getByteArray()));
    }
}
