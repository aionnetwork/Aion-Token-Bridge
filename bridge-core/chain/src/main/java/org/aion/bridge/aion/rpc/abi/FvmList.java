
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

import org.aion.bridge.chain.base.utility.ByteUtils;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FvmList extends FvmBaseType {

    List<FvmBaseType> params;

    public FvmList() {this.params = new ArrayList<>();};

    public FvmList(@Nonnull final FvmBaseType...params) {
        this.params = new ArrayList<>(Arrays.asList(params));
    }

    public void add(FvmBaseType param) {
        this.params.add(param);
    }

    @Override
    public byte[] serialize() {
        ByteBuffer bb = ByteBuffer.allocate(params.size() * params.get(0).serialize().length + 16);
        int elementLength = params.size();
        bb.put(ByteUtils.pad(BigInteger.valueOf(elementLength).toByteArray(), 16));

        for (FvmBaseType p : params) {
            bb.put(p.serialize());
        }
        return bb.array();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public Optional<List<FvmBaseType>> getEntries() {
        return Optional.of(this.params);
    }
}
