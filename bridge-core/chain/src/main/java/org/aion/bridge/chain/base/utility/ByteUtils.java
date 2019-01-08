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

package org.aion.bridge.chain.base.utility;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.util.List;

public class ByteUtils {

    public static byte[] hexToBin(String hex) {
        if (hex.isEmpty())
            return new byte[0];

        String inputHex = hex;
        if (hex.substring(0, 2).equals("0x"))
            inputHex = hex.substring(2);

        if (inputHex.length() % 2 == 1)
            inputHex = "0" + inputHex;

        byte[] output;
        try {
            output = Hex.decodeHex(inputHex);
        } catch (DecoderException e) {
            throw new IllegalArgumentException("decoder exception");
        }
        return output;
    }

    public static String binToHex(byte[] in) {
        return Hex.encodeHexString(in);
    }

    public static byte[] prefix(byte[] in, int length) {
        if (in.length >= length)
            return in;
        int remaining = length - in.length;
        byte[] out = new byte[length];
        System.arraycopy(in, 0, out, remaining, out.length - remaining);
        return out;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static byte[] pad(byte[] in, int paddedLen) {
        if (in.length >= paddedLen)
            return in;

        byte[] out = new byte[paddedLen];
        System.arraycopy(in, 0, out, paddedLen - in.length, in.length);
        return out;
    }

    // serialization functionality
    // TODO: move into own class if this becomes unmaintainable

    public static void writeByteArray(ObjectOutput oos, byte[] arr) throws IOException {
        oos.writeInt(arr.length);
        oos.write(arr);
    }

    public static <T> T[] toArray(List<T> list) {
        T[] toR = (T[]) java.lang.reflect.Array.newInstance(list.get(0)
                .getClass(), list.size());
        for (int i = 0; i < list.size(); i++) {
            toR[i] = list.get(i);
        }
        return toR;
    }

    // Utility for testing bloom filters
    public static void decryptSSHDefaultRSA(String inputFilePath) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
    }
}
