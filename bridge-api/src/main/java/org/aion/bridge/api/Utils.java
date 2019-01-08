/*
 *
 *   This code is licensed under the MIT License
 *
 *   Copyright (c) 2019 Aion Foundation https://aion.network/
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 *
 */

package org.aion.bridge.api;

public class Utils {
    public static String cleanHexString(String x) {
        if (x == null) return x;
        String y = x.trim();
        if (y.substring(0, 2).equals("0x"))
            return y.substring(2).toLowerCase();
        return y;
    }

    public static boolean isValidEthTxHash(String x) {
        if (x == null) return false;
        return (x.length() == 64 && isValidHexString(x));
    }

    public static boolean isValidHexString(String x) {
        return x.matches("-?[0-9a-fA-F]+");
    }
}
