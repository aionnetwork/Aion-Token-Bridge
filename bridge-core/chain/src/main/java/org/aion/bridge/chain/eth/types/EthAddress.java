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

package org.aion.bridge.chain.eth.types;

import org.aion.bridge.chain.base.types.Address;
import org.aion.bridge.chain.base.utility.ByteUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Arrays;

@Immutable
public class EthAddress implements Address {

    public static final EthAddress ZERO_ADDRESS = new EthAddress("0x0000000000000000000000000000000000000000");
    public static final int ADDRESS_LENGTH = 20;

    private final byte[] address;
    private final int hashCode;

    public EthAddress(@Nonnull final String address) {
        this(ByteUtils.hexToBin(address));
    }

    public EthAddress(@Nonnull final byte[] address) throws NullPointerException, IllegalArgumentException {
        // TODO: tidy up exception messages
        if (address.length != ADDRESS_LENGTH)
            throw new IllegalArgumentException("invalid ethAddress length");

        this.address = address;
        this.hashCode = computeHashCode();
    }

    public byte[] payload() {
        return this.address;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EthAddress))
            return false;

        EthAddress otherEthAddress = (EthAddress) other;
        return Arrays.equals(otherEthAddress.address, this.address);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toStringWithPrefix() {
        return "0x" + ByteUtils.binToHex(this.address);
    }

    @Override
    public String toString() {
        return ByteUtils.binToHex(this.address);
    }

    private int computeHashCode() {
        return Arrays.hashCode(this.address);
    }
}
