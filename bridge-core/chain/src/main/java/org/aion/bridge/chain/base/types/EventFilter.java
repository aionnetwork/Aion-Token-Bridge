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

import javax.annotation.Nonnull;

public abstract class EventFilter {

    protected final Bloom addressEventBloom;
    private final Address contractAddress;
    private final String event;
    private Word32 eventHash;

    public EventFilter(@Nonnull final Address contractAddress,
                        @Nonnull final String event,
                       @Nonnull final Bloom bloom) {
        this.contractAddress = contractAddress;
        this.event = event;

        this.addressEventBloom = bloom;
        this.addressEventBloom.add(contractAddress.payload());

        this.eventHash = getEventSignatureHash(event);
        this.addressEventBloom.add(this.eventHash.payload());
    }

    public EventFilter(@Nonnull final Address contractAddress,
                       @Nonnull final Word32 eventHash,
                       @Nonnull final Bloom bloom) {
        this.contractAddress = contractAddress;

        this.addressEventBloom = bloom;
        this.addressEventBloom.add(contractAddress.payload());

        this.event = null;

        this.eventHash = eventHash;
        this.addressEventBloom.add(this.eventHash.payload());
    }

    /**
     * Support arbitrary hashing function to transform event signature string to hash
     */
    protected abstract Word32 getEventSignatureHash(String event);

    public boolean matches(Bloom bloom) { return bloom.has(addressEventBloom); }

    public Address getContractAddress() {
        return contractAddress;
    }

    public Word32 getEventHash() {
        return eventHash;
    }

    public boolean contains(@Nonnull Bloom input) {
        return addressEventBloom.has(input);
    }
}
