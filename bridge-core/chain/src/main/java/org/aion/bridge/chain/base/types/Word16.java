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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.aion.bridge.chain.base.serialize.Word16Deserializer;
import org.aion.bridge.chain.base.serialize.Word16Serializer;
import org.aion.bridge.chain.base.utility.ByteUtils;

import javax.annotation.concurrent.Immutable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;


@JsonSerialize(using = Word16Serializer.class)
@JsonDeserialize(using = Word16Deserializer.class)
@Immutable
public class Word16 {
    public static final int LENGTH = 16; // haha

    @JsonProperty
    private final ImmutableBytes payload;

    @JsonIgnore
    private final int hashcode;

    public Word16(long input) {
        this(ByteUtils.pad(BigInteger.valueOf(input).toByteArray(), 16));
    }

    public Word16(String payload) {
        this(ByteUtils.hexToBin(payload));
    }

    public Word16(byte[] payload) {
        Objects.requireNonNull(payload);

        if (payload.length != LENGTH)
            throw new IllegalArgumentException("word16 length must be 16");

        this.payload = new ImmutableBytes(payload);
        this.hashcode = Arrays.hashCode(payload);
    }

    public Word16(ImmutableBytes payload) {
        Objects.requireNonNull(payload);

        if (payload.getLength() != LENGTH)
            throw new IllegalArgumentException("word16 length must be 16");

        this.payload = payload;
        this.hashcode = payload.hashCode();
    }

    public Word16(Word32 word) {
        this (getLowerHalf(word.payload()));
    }

    public byte[] getPayload() {
        return this.payload.getByteArray();
    }

    public byte[] payload() {
        return this.payload.getByteArray();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Word16))
            return false;

        Word16 otherWord16 = (Word16) other;
        return this.payload.equals(otherWord16.payload);
    }

    @Override
    public int hashCode() {
        return this.hashcode;
    }

    @Override
    public String toString() {
        return payload.toString();
    }

    public String toStringWithPrefix() {
        return payload.toStringWithPrefix();
    }

    // chops the byte array in half, taking the lower half
    private static byte[] getLowerHalf(byte[] input) {
        int outLen = input.length / 2;
        byte[] out = new byte[outLen];
        System.arraycopy(input, outLen, out, 0, outLen);
        return out;
    }
}
