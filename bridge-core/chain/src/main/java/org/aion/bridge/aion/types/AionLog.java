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

import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Log;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.CryptoUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class AionLog implements Log {
    private final Word32 aionAddress;
    private final ImmutableBytes data;
    private final List<Word32> topics;
    private final int hashCode;
    private final Word32 bytesEncoded;

    public AionLog(Word32 aionAddress, ImmutableBytes data, List<Word32> topics) {
        this.aionAddress = aionAddress;
        this.topics = topics;
        this.data = data;
        this.bytesEncoded = computeByteEncoded();
        this.hashCode = Arrays.hashCode(this.bytesEncoded.payload());
    }

    public Word32 getAddress() { return aionAddress; }
    public ImmutableBytes getData() { return data; }
    public List<Word32> getTopics() { return topics; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AionLog)) return false;
        if (!this.aionAddress.equals(((AionLog) o).getAddress())) return false;
        if (!this.data.equals(((AionLog) o).getData())) return false;
        if(topics.size() != ((AionLog) o).getTopics().size()) return false;
        for(int i = 0; i < topics.size(); i++)
            if (!this.topics.get(i).equals(((AionLog) o).getTopics().get(i))) return false;

        return true;
    }

    @Override
    public int hashCode() { return hashCode; }

    public Word32 getBytesEncoded() { return bytesEncoded; }

    private Word32 computeByteEncoded(){

        final int BYTE_ENCODED_LENGTH = 32 + (int) data.getLength() + topics.size() * 32 ;

        ByteBuffer buf = ByteBuffer.allocate(BYTE_ENCODED_LENGTH);
        buf.put(aionAddress.payload());
        buf.put(data.getByteArray());
        for (Word32 topic : topics) buf.put(topic.payload());
        return new Word32(CryptoUtils.blake2b256(buf.array()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("aionAddress: ");
        sb.append(aionAddress);
        sb.append(", data: ");
        sb.append(data.toString());
        sb.append(", bytesEncoded: ");
        sb.append(bytesEncoded.toString());
        sb.append(", topics: ");
        for(Word32 topic : topics)
            sb.append(topic.toString());

        return sb.toString();
    }

    private int computeHashCode() {
        return Arrays.hashCode(this.computeByteEncoded().payload());
    }
}