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

import org.aion.bridge.chain.base.utility.ByteUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public abstract class Bloom {
    public static int BLOOM_SIZE = 256;
    public static int HEADER_BYTES = 6;

    public byte[] payload;

    public BigInteger payloadBI;

    public Bloom() {
        this.payload = new byte[BLOOM_SIZE];
        this.payloadBI = BigInteger.ZERO;
    }

    public Bloom(String payload) {
        this (ByteUtils.hexToBin(payload));
    }

    public Bloom(byte[] payload) {
        Objects.requireNonNull(payload);

        if (payload.length != BLOOM_SIZE)
            throw new IllegalArgumentException("bloom filter must be of length 256");
        this.payload = payload;
        this.payloadBI = new BigInteger(payload);
    }

    /**
     * Since our two implementations use different hashing functions, abstract here
     * @param input
     * @return
     */
    protected abstract byte[] hash(byte[] input);

    /**
     *
     * Check if bloom matches with a event value, note that working this way is not very efficient, instead
     * prefer the alternative {@code hasValue(byte[] value)}, which expects the hash directly.
     *
     * @param value any string input indicating the desired element
     * @return {@code true} if element might exist in bloom, {@code false} otherwise
     */
    public boolean hasEvent(String value) {
        byte[] eventHash = hash(hash(value.getBytes()));
        return verifyBloom(this.payloadBI, eventHash);
    }

    /**
     * Checks if a particular ethAddress may be inside the block
     *
     * @param addr desired Address to be checked
     * @return {@code true} if element might exist in bloom {@code false} otherwise
     */
    public boolean hasAddress(Address addr) {
        return verifyBloom(this.payloadBI, hash(addr.payload()));
    }

    public boolean has(Bloom bloom) {
        return this.payloadBI.and(bloom.payloadBI).compareTo(bloom.payloadBI) == 0;
    }

    /**
     * In-place or
     * @param bloom the other bloom object to be or'd against
     */
    public void or(Bloom bloom) {
        this.payloadBI = this.payloadBI.or(bloom.payloadBI);
        this.payload = ByteUtils.prefix(this.payloadBI.toByteArray(), 256);
    }

    private void updatePayload(byte[] hash) {
        BigInteger bi = toBloomBits(this.payloadBI, hash);
        this.payloadBI = bi;
        this.payload = ByteUtils.prefix(bi.toByteArray(), 256);
    }

    /**
     * Adds an event to the bloom filter
     * @param value any string input indicating the element to be added
     */
    public void add(String value) {
        byte[] hash = hash(hash(value.getBytes()));
        updatePayload(hash);
    }

    /**
     * Adds an ethAddress to the bloom filter
     * @param addr any ethAddress that should be apart of the bloom filter
     */
    public void add(Address addr) {
        byte[] hash = hash(addr.payload());
        updatePayload(hash);
    }

    public void add(byte[] data) {
        byte[] hash = hash(data);
        updatePayload(hash);
    }

    public void add(Word32 data) {
        add(data.payload());
    }

    public void addHex(String hex) {
        byte[] hash = hash(ByteUtils.hexToBin(hex));
        updatePayload(hash);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Bloom))
            return false;

        Bloom otherBloom = (Bloom) other;
        return Arrays.equals(otherBloom.payload, this.payload);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(payload);
    }

    private static BigInteger twoBytesToBloom(int high, int low) {
        return BigInteger.ONE.shiftLeft((low + (high << 8)) & 2047);
    }

    private static boolean verifyBloom(BigInteger bloom, byte[] data) {
        int i = 0;
        while (i < HEADER_BYTES) {
            int high = data[i++] & 0xFF;
            int low = data[i++] & 0xFF;
            BigInteger bloomBit = twoBytesToBloom(high, low);
            if (bloom.and(bloomBit).compareTo(BigInteger.ZERO) == 0) {
                return false;
            }
        }
        return true;
    }

    private static BigInteger toBloomBits(BigInteger prevBI, byte[] data) {
        int i = 0;
        BigInteger bi = prevBI;
        while (i < HEADER_BYTES) {
            int high = data[i++] & 0xFF;
            int low = data[i++] & 0xFF;
            bi = bi.or(twoBytesToBloom(high, low));
        }
        return bi;
    }

    // cache
    private volatile String payloadCache;

    @Override
    public String toString() {
        if (payloadCache == null)
            payloadCache = ByteUtils.binToHex(payload);
        return payloadCache;
    }
}
