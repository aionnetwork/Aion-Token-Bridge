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

import java.util.Arrays;
import java.util.Objects;

public class ImmutableBytes {

    private final byte[] payload;

    public ImmutableBytes(String hex) {
        this(ByteUtils.hexToBin(hex));
    }

    public ImmutableBytes(byte[] bytes) {
        Objects.requireNonNull(bytes);
        this.payload = new byte[bytes.length];
        System.arraycopy(bytes, 0, this.payload, 0, bytes.length);
    }

    public byte[] getByteArray() {
        byte[] out = new byte[this.payload.length];
        System.arraycopy(this.payload, 0, out, 0, this.payload.length);
        return out;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(payload);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ImmutableBytes))
            return false;
        return Arrays.equals(this.payload, ((ImmutableBytes) other).payload);
    }

    @Override
    public String toString() {
        return ByteUtils.binToHex(payload);
    }

    public String toStringWithPrefix() {
        return "0x" + ByteUtils.binToHex(payload);
    }

    public long getLength() {
        return this.payload.length;
    }
}
