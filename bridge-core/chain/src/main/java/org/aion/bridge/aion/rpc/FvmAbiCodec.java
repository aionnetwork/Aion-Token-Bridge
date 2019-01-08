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

package org.aion.bridge.chain.aion.rpc;

import org.aion.bridge.chain.aion.rpc.abi.FvmBaseType;
import org.aion.bridge.chain.base.types.Word16;
import org.aion.bridge.chain.base.utility.ByteUtils;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ThreadSafe
public class FvmAbiCodec {
    private List<FvmBaseType> params;
    private volatile StringBuffer buffer;
    private String signature;

    private static final int STATIC_OFFSET_LEN = 16;

    public FvmAbiCodec(@Nonnull String signature, @Nonnull FvmBaseType...params) {
        this.params = new ArrayList<>(Arrays.asList(params));
        this.signature = signature;
    }

    public synchronized FvmAbiCodec setParam(FvmBaseType param) {
        this.params.add(param);
        return this;
    }

    private synchronized void createBuffer() {
        // represents the offsets until dynamic parameters
        int offset = 0;
        final StringBuffer b = new StringBuffer();

        b.append(ByteUtils.binToHex(encodeSignature(this.signature)));

        for (FvmBaseType type : this.params) {
            if (type.isDynamic()) {
                offset += 16;
            } else {
                offset += type.serialize().length;
            }
        }

        // second iteration, go through each element assembling
        for (FvmBaseType type : this.params) {
            if (type.isDynamic()) {
                b.append(new Word16(offset).toString());
                offset += type.serialize().length;
            } else {
                b.append(ByteUtils.binToHex(type.serialize()));
            }
        }

        // in the last iteration just iterate through dynamic elements
        for (FvmBaseType type : this.params) {
            if (type.isDynamic()) {
                b.append(ByteUtils.binToHex(type.serialize()));
            }
        }

        this.buffer = b;
    }

    public String encode() {
        if (buffer == null)
            createBuffer();
        return "0x" + buffer.toString();
    }

    @Override
    public String toString() { return encode(); }

    private static byte[] encodeSignature(String s) {
        // encode signature
        byte[] sig = new byte[4];
        System.arraycopy(CryptoUtils.keccak256(s.getBytes()), 0, sig, 0, 4);
        return sig;
    }
}
