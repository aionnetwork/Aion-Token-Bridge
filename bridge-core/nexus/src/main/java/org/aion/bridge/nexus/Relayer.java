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

package org.aion.bridge.nexus;

import org.aion.bridge.chain.aion.types.AionAddress;

import javax.annotation.Nonnull;
import java.math.BigInteger;

public class Relayer {

    private AionAddress accountAddress;
    private String publicKey;
    private Enclave enclaveClient;
    private NonceManager nonceManager;

    public Relayer(@Nonnull Enclave enclaveClient,
                   @Nonnull AionAddress accountAddress,
                   @Nonnull String publicKey) {

        this.enclaveClient = enclaveClient;
        this.accountAddress = accountAddress;
        this.publicKey = publicKey;
        this.nonceManager = new NonceManager();
    }

    public String getPublicKey() {
        return publicKey;
    }

    public AionAddress getAccountAddress() {
        return accountAddress;
    }

    public Enclave getEnclaveClient() {
        return enclaveClient;
    }

    public BigInteger getNonce(){
        return this.nonceManager.getRelayerNonce();
    }

    public void setNonce(BigInteger newNonce){
        this.nonceManager.setRelayerNonce(newNonce);
    }

}
