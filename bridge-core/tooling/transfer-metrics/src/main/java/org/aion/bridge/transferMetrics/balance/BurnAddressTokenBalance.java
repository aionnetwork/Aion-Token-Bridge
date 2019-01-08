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

package org.aion.bridge.transferMetrics.balance;

import org.aion.bridge.chain.aion.rpc.FvmAbiCodec;
import org.aion.bridge.chain.aion.rpc.abi.VMEthAddress;
import org.aion.bridge.chain.base.api.IncompleteApiCallException;
import org.aion.bridge.chain.base.api.MalformedApiResponseException;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;

import java.math.BigInteger;

public class BurnAddressTokenBalance {
    private static EthAddress burnAddress = new EthAddress("0x0000000000000000000000000000000000000000");
    private EthAddress contractAddress;
    private StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> ethJsonRpcConnection;


    public BurnAddressTokenBalance(StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> ethConnection, EthAddress contractAddress) {
        this.ethJsonRpcConnection = ethConnection;
        this.contractAddress = contractAddress;
    }

    public BigInteger getBurnAddressBalance() {
        BigInteger bal = BigInteger.ZERO;
        int err_count = 0;
        while (bal.equals(BigInteger.ZERO) && err_count < 5) {
            try {
                ImmutableBytes txnData = new ImmutableBytes(
                        new FvmAbiCodec("balanceOf(address)", new VMEthAddress(burnAddress)).encode());
                String b = ethJsonRpcConnection.contractCall(contractAddress, txnData);
                bal = new BigInteger(b.substring(2), 16);

            } catch (IncompleteApiCallException | MalformedApiResponseException |QuorumNotAvailableException | InterruptedException e) {
                e.printStackTrace();
                err_count++;
            }
        }
        return bal;
    }
}