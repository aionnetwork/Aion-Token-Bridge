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

import org.aion.bridge.chain.base.utility.CryptoUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class AccountState<A extends Address> {

    private final A address;
    private final long blockNumber;
    private final BigInteger balance;
    private final BigInteger nonce;

    private final int hashCode;

    public AccountState(A address, long blockNumber, BigInteger balance, BigInteger nonce) {
        this.address = address;
        this.blockNumber = blockNumber;
        this.balance = balance;
        this.nonce = nonce;

        this.hashCode = Arrays.hashCode(this.computeStateHash());
    }

    public A getAddress() { return address; }
    public long getBlockNumber() { return blockNumber; }
    public BigInteger getBalance() { return balance; }
    public BigInteger getNonce() { return nonce; }


    @Override
    public int hashCode() { return hashCode; }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AccountState))
            return false;

        AccountState o = (AccountState) other;

        if (!this.address.equals(o.address))
            return false;
        if (this.blockNumber != o.blockNumber)
            return false;
        if (!this.balance.equals(o.balance))
            return false;
        //noinspection RedundantIfStatement
        if (!this.nonce.equals(o.nonce))
            return false;

        return true;
    }

    @SuppressWarnings("Duplicates")
    private byte[] computeStateHash() {
        // address +
        byte[] address = this.address.payload();
        byte[] balance = this.balance.toByteArray();
        byte[] nonce = this.nonce.toByteArray();

        final int SIZE = address.length + 8 + balance.length + nonce.length;
        ByteBuffer buf = ByteBuffer.allocate(SIZE);

        buf.put(address);
        buf.putLong(blockNumber);
        buf.put(balance);
        buf.put(nonce);
        return CryptoUtils.blake2b256(buf.array());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("address: ");
        sb.append(address);
        sb.append(", blockNumber: ");
        sb.append(blockNumber);
        sb.append(", balance: ");
        sb.append(balance);
        sb.append(", nonce: ");
        sb.append(nonce);
        return sb.toString();
    }
}
