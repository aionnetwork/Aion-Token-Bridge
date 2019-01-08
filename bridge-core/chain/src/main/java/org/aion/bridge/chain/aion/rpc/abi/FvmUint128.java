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

package org.aion.bridge.chain.aion.rpc.abi;

import org.aion.bridge.chain.base.types.Word16;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.ByteUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class FvmUint128 extends FvmBaseType {

    private final Word16 payload;

    public FvmUint128(@Nonnull final Word16 word) {
        this.payload = word;
    }

    public FvmUint128(@Nonnull final Word32 word) {
        this.payload = new Word16(word);
    }

    @Override
    public byte[] serialize() {
        return this.payload.payload();
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public Optional<List<FvmBaseType>> getEntries() {
        return Optional.empty();
    }
}
