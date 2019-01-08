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

import org.libsodium.jni.Sodium;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class SodiumLoader {
    static {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if(os.contains("win")) {
            //loadLibrary("libjsodium.dll");
            throw new RuntimeException("failed to load libsodium, OS isn't supported");
        } else if(os.contains("mac") || os.contains("darwin")) {
            throw new RuntimeException("failed to load libsodium, OS isn't supported");
        } else if(os.contains("linux")) {
            loadLibrary("native/libsodium.so");
            loadLibrary("native/libsodiumjni.so");
        } else {
            throw new RuntimeException("failed to load libsodium, OS isn't supported");
        }
    }

    private static void loadLibrary(String file) {
        final String[] parts = file.split("\\.", 2);
        final String name = parts[0];
        final String extension = parts[1];


        try {
            final File tempFile = File.createTempFile(name, extension);
            tempFile.deleteOnExit();

            try(
                    InputStream inputStream = Sodium.class.getClassLoader().getResourceAsStream(file);
                    OutputStream outputStream = new FileOutputStream(tempFile)
            ) {
                byte[] buffer = new byte[4096];
                int len;
                while((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
            }

            System.load(tempFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static volatile boolean sodiumInitialized = false;

    // JDK 1.5+ only
    public static Sodium sodium() {
        if (!sodiumInitialized) {
            synchronized(SodiumLoader.class) {
                Sodium.sodium_init();
                sodiumInitialized = true;
            }
        }
        return SodiumLoader.SingletonHolder.SODIUM_INSTANCE;
    }

    private static final class SingletonHolder {
        public static final Sodium SODIUM_INSTANCE = new Sodium();
    }
}
