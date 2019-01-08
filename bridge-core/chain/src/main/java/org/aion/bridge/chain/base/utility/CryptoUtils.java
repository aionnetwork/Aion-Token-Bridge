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

package org.aion.bridge.chain.base.utility;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.jcajce.provider.digest.Keccak;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtils {
    private static ThreadLocal<MessageDigest> digest = new ThreadLocal<MessageDigest>() {
        @Override
        public MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("no SHA-256 algorithm found, this should never happen", e);
            }
        }
    };

    private static ThreadLocal<Keccak.Digest256> keccak256Digest = new ThreadLocal<>() {
        @Override
        public Keccak.Digest256 initialValue() {
            return new Keccak.Digest256();
        }
    };

    private static ThreadLocal<Digest> blake2B256Digest = new ThreadLocal<>() {
        @Override
        public Digest initialValue() {
            try {
                return new Blake2bDigest(256);
            } catch (Exception e) {
                throw new RuntimeException("no blake2b 256 algorithm found, this should never happen", e);
            }
        }
    };

    public static byte[] keccak256(byte[] in) {
        Keccak.Digest256 digest = keccak256Digest.get();
        digest.reset();
        return digest.digest(in);
    }

    public static byte[] blake2b256(byte[] in) {
        Digest digest = blake2B256Digest.get();
        digest.reset();
        digest.update(in, 0, in.length);

        byte[] out = new byte[32];
        digest.doFinal(out, 0);
        return out;
    }

    // hashing functionality
    public static byte[] sha256(byte[] input) {
        MessageDigest d = digest.get();
        d.reset();
        return d.digest(input);
    }
}
