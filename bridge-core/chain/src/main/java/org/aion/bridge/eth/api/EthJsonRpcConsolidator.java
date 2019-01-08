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

package org.aion.bridge.chain.eth.api;

import org.aion.bridge.chain.base.api.ConsolidatedChainConnection;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class EthJsonRpcConsolidator {
    private ConsolidatedChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> api;

    public EthJsonRpcConsolidator(List<StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress>> connections, int quorumSize, ThreadPoolExecutor executor) {
        api = new ConsolidatedChainConnection<>(connections, quorumSize, executor);
    }

    public EthJsonRpcConsolidator(List<StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress>> connections, int quorumSize, Duration timeout, ThreadPoolExecutor executor) {
        api = new ConsolidatedChainConnection<>(connections, quorumSize, timeout, executor);
    }

    public ConsolidatedChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> getApi() {
        return api;
    }
}
