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

import org.aion.bridge.chain.base.utility.ByteUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Base type for a numerical value derived from some JSON string, or vice versa
 */
public class NumericalValue {

    private static final Pattern numericPattern = Pattern.compile("(0x)[0-9a-fA-F]+$");

    public static NumericalValue EMPTY = new NumericalValue("");
    private final BigInteger value;
    private String cachedStringValue;

    private int hashCode;

    public NumericalValue(String in) {
        if (in.isEmpty()) {
            value = BigInteger.ZERO;
            return;
        }

        if (numericPattern.matcher(in).matches()) {
            // hexadecimal string
            value = new BigInteger(1, (ByteUtils.hexToBin(in)));
        } else {
            // otherwise assume that this is an numeric string
            value = new BigInteger(in, 10);
        }

        this.hashCode = Arrays.hashCode(value.toByteArray());
    }

    public NumericalValue(long in) {
        this.value = BigInteger.valueOf(in);
    }

    public NumericalValue(BigInteger in) {
        this.value = in;
    }

    public NumericalValue(byte[] in) {
        this.value = new BigInteger(1, in);
    }

    private void generateIntermediateState() {
        if (this.cachedStringValue == null)
            this.cachedStringValue = "0x" + ByteUtils.binToHex(this.value.toByteArray());
    }

    public String toHexString() {
        generateIntermediateState();
        return this.cachedStringValue;
    }

    public BigInteger toBigInteger() {
        return this.value;
    }

    @Override
    public String toString() {
        return toHexString();
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}