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

package org.aion.monitor.api;

import org.aion.bridge.chain.base.api.IncompleteApiCallException;
import org.aion.bridge.chain.base.api.MalformedApiResponseException;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.eth.api.EthJsonRpcConnection;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;

import javax.annotation.Nonnull;
import java.util.Optional;

public class EthJsonRpcApi implements EthApi{

    private StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> ethNodeConnection;
    private int id;
    private String url;
    private String name;
    private static int maxRetryCount = 5;

    public EthJsonRpcApi(@Nonnull final String url,
                         @Nonnull final String name,
                         @Nonnull final int id) {

        this.ethNodeConnection = new EthJsonRpcConnection(url);
        this.url = url;
        this.name = name;
        this.id = id;
    }

    public String getUrl() { return url; }
    public String getName() {
        return name;
    }
    public Integer getId() {
        return id;
    }

    public Long getLatestBLockNumber() {

        int retries;
        Long latestBlockNumber = null;

        for (retries = 0; retries < maxRetryCount; retries++) {
            try {
                latestBlockNumber = ethNodeConnection.getBlockNumber();
                if (latestBlockNumber != null) {
                    break;
                }

            } catch (IncompleteApiCallException | MalformedApiResponseException | QuorumNotAvailableException | InterruptedException e) {
                System.err.println("Error occurred in [getBlockNumber], url: " + url);
            }
        }
        return latestBlockNumber;
    }

    public Optional<EthBlock> getBLock(Long blockNumber) {
        if (blockNumber == null)
            return Optional.empty();

        int retries;
        Optional<EthBlock> block = Optional.empty();
        for (retries = 0; retries < maxRetryCount; retries++) {
            try {
                block = ethNodeConnection.getBlock(blockNumber);
                if (block.isPresent()) {
                    break;
                }
            } catch (IncompleteApiCallException | MalformedApiResponseException | QuorumNotAvailableException | InterruptedException e) {
                System.err.println("Error occurred in [getBlock], url: " + url);
            }
        }
        return block;
    }

    public Integer getPeerCount() {
        int count = -1;
        try {
            count = ethNodeConnection.peerCount();
        } catch (IncompleteApiCallException | MalformedApiResponseException | QuorumNotAvailableException | InterruptedException e) {
            System.err.println("Error occurred in [peerCount], url: " + url);
            e.printStackTrace();
        }

        return count;
    }

}

