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

package org.aion.bridge.chain.aion.types;

import org.aion.bridge.chain.base.types.Address;
import org.aion.bridge.chain.base.types.Bloom;
import org.aion.bridge.chain.base.types.EventFilter;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import javax.annotation.Nonnull;

public class AionEventFilter extends EventFilter {
    public AionEventFilter(@Nonnull final Address contractAddress,
                           @Nonnull final String event,
                           @Nonnull final Bloom bloom) {
        super(contractAddress, event, bloom);
    }

    public AionEventFilter(@Nonnull final Address contractAddress,
                          @Nonnull final Word32 eventHash,
                          @Nonnull final Bloom bloom) {
        super(contractAddress, eventHash, bloom);
    }

    @Override
    protected Word32 getEventSignatureHash(String event) {
        return new Word32(CryptoUtils.keccak256(event.getBytes()));
    }
}
