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
 *        public Request(AionAddress address, String blockId) {

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.aion.bridge.chain.aion.rpc;

import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.SodiumLoader;
import org.aion.rlp.RLP;

import javax.annotation.Nonnull;
import java.math.BigInteger;

public class AionRawTransactionCodec {
    private static byte _type = (byte) 0x1;
    public static int PUBLIC_KEY_LEN = SodiumLoader.sodium().crypto_sign_ed25519_publickeybytes();
    public static int SIGNATURE_LEN = SodiumLoader.sodium().crypto_sign_ed25519_bytes();

    public static byte[] getRLPRaw(@Nonnull final Word32 to,
                                   @Nonnull final BigInteger value,
                                   @Nonnull final ImmutableBytes data,
                                   @Nonnull final BigInteger nonce,
                                   @Nonnull final BigInteger timestamp,
                                   @Nonnull final long energy,
                                   @Nonnull final long energyPrice) {

        byte[] _nonce = nonce.toByteArray();
        Word32 _to = to;
        byte[] _value = value.toByteArray();
        byte[] _data = data.getByteArray();
        byte[] _timestamp = timestamp.toByteArray();
        long _nrg = energy;
        long _nrgPrice = energyPrice;

        byte[] rlpRaw = RLP.encodeList(RLP.encodeElement(_nonce),
                _to == null ? RLP.encodeElement(null) : RLP.encodeElement(_to.payload()),
                RLP.encodeElement(_value),
                RLP.encodeElement(_data),
                RLP.encodeElement(_timestamp),
                RLP.encodeLong(_nrg),
                RLP.encodeLong(_nrgPrice),
                RLP.encodeByte(_type)
        );
        return rlpRaw;
    }

    public static byte[] getRLPFinal(@Nonnull final Word32 to,
                                     @Nonnull final BigInteger value,
                                     @Nonnull final ImmutableBytes signedData,
                                     @Nonnull final BigInteger nonce,
                                     @Nonnull final BigInteger timestamp,
                                     @Nonnull final long energy,
                                     @Nonnull final long energyPrice,
                                     @Nonnull byte[] publicKey,
                                     @Nonnull byte[] signature) {
        byte[] _timestamp = timestamp.toByteArray();
        byte[] _nonce = nonce.toByteArray();
        long _nrg = energy;
        long _nrgPrice = energyPrice;
        byte[] _data = signedData.getByteArray();

        byte[] pkSig = new byte[PUBLIC_KEY_LEN + SIGNATURE_LEN];
        System.arraycopy(publicKey, 0, pkSig, 0, PUBLIC_KEY_LEN);

        System.arraycopy(signature, 0, pkSig, PUBLIC_KEY_LEN, SIGNATURE_LEN);

        byte[] rlpFinal = RLP.encodeList(RLP.encodeElement(_nonce),
                to == null ? RLP.encodeElement(null) : RLP.encodeElement(to.payload()),
                RLP.encodeElement(value.toByteArray()),
                RLP.encodeElement(_data),
                RLP.encodeElement(_timestamp),
                RLP.encodeLong(_nrg),
                RLP.encodeLong(_nrgPrice),
                RLP.encodeByte(_type),
                RLP.encodeElement(pkSig)
        );

        return rlpFinal;
    }
}
