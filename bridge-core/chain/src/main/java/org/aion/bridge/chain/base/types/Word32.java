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
import org.aion.bridge.chain.base.serialize.Word32Deserializer;
import org.aion.bridge.chain.base.serialize.Word32Serializer;
import org.aion.bridge.chain.base.utility.ByteUtils;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Used mostly for representing hashes, or similar 32-byte constructs
 */
@JsonSerialize(using = Word32Serializer.class)
@JsonDeserialize(using = Word32Deserializer.class)
@Immutable
public class Word32 implements Address {
    public static final int LENGTH = 32;
    public static final Word32 EMPTY = new Word32(new byte[32]);

    @JsonProperty
    private final ImmutableBytes payload;

    @JsonIgnore
    private final int hashcode;

    public Word32(String payload) {
        this (ByteUtils.hexToBin(payload));
    }

    public Word32(Word32 copy) {
        Objects.requireNonNull(copy);

        this.payload = copy.payloadImmutableBytes();
        this.hashcode = copy.hashCode();
    }

    public Word32(byte[] payload) {
        Objects.requireNonNull(payload);

        if (payload.length != LENGTH)
            throw new IllegalArgumentException("word32 length must be 32 bytes");

        this.payload = new ImmutableBytes(payload);
        this.hashcode = Arrays.hashCode(payload);
    }

    public Word32(ImmutableBytes payload) {
        Objects.requireNonNull(payload);

        if (payload.getLength() != LENGTH)
            throw new IllegalArgumentException("word32 length must be 32 bytes");

        this.payload = payload;
        this.hashcode = payload.hashCode();
    }

    public byte[] payload() {
        return this.payload.getByteArray();
    }

    public ImmutableBytes payloadImmutableBytes() {
        return this.payload;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Word32))
            return false;

        Word32 otherWord32 = (Word32) other;
        return this.payload.equals(otherWord32.payload);
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
}
