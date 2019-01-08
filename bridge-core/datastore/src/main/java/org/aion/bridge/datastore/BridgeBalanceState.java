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

package org.aion.bridge.datastore;

import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.aion.types.AionBlock;
import org.aion.bridge.chain.aion.types.AionLog;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.api.ConsolidatedChainConnection;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.bridge.CriticalBridgeTaskException;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.ObjectUtils.allNotNull;

public class BridgeBalanceState extends Thread {
    private final Logger log = LoggerFactory.getLogger(BridgeBalanceState.class);

    private volatile boolean shutdown = false;
    private volatile BigInteger bridgeBalance = null;
    private volatile BigInteger relayerBalance = null;

    private final TimeUnit pollIntervalTimeUnit;
    private final long pollInterval;
    private final ConsolidatedChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> api;
    private final AionTipState tipState;
    private final AionAddress bridgeAddress;
    private final AionAddress relayerAddress;

    private DataStore ds = null;

    private static final int MAX_CONSECUTIVE_ERR = 10;

    public static class Builder {

        // Required parameters
        ConsolidatedChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> api;
        AionTipState tipState;
        AionAddress bridgeAddress;
        AionAddress relayerAddress;

        // Optional parameters
        long pollInterval = 1;
        TimeUnit pollIntervalTimeUnit = TimeUnit.MINUTES;
        DataStore ds = null;

        public Builder setPollInterval(long pollInterval, TimeUnit timeUnit) {
            this.pollInterval = pollInterval;
            this.pollIntervalTimeUnit = timeUnit;
            return this;
        }

        public Builder setApi(ConsolidatedChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> x) { api = x; return this; }
        public Builder setTipState(AionTipState x) { tipState = x; return this; }
        public Builder setBridgeAddress(AionAddress x) { bridgeAddress = x; return this; }
        public Builder setRelayerAddress(AionAddress x) { relayerAddress = x; return this; }
        public Builder setDatabase(DataStore x) { ds = x; return this; }

        public BridgeBalanceState build() throws SQLException {
            if (allNotNull(api, tipState, bridgeAddress, relayerAddress))
                return new BridgeBalanceState(this);

            else throw new IllegalStateException();
        }
    }

    private BridgeBalanceState(Builder b) {
        this.pollInterval = b.pollInterval;
        this.pollIntervalTimeUnit = b.pollIntervalTimeUnit;

        this.api = b.api;
        this.tipState = b.tipState;
        this.bridgeAddress = b.bridgeAddress;
        this.relayerAddress = b.relayerAddress;

        this.ds = b.ds;
    }

    public Optional<BigInteger> getBridgeBalance() { return Optional.ofNullable(bridgeBalance); }

    public Optional<BigInteger> getRelayerBalance() {
        return Optional.ofNullable(relayerBalance);
    }

    public Optional<BigInteger> getBalance(AionAddress address){
        if(address.equals(bridgeAddress)) return Optional.ofNullable(bridgeBalance);
        else if(address.equals(relayerAddress)) return Optional.ofNullable(relayerBalance);
        else return Optional.empty();
    }

    @Override
    public void run() {
        int errAccumulator = 0;
        while (!shutdown && errAccumulator < MAX_CONSECUTIVE_ERR) {
            try {
                Optional<Long> _blockNumber = tipState.getBlockNumber();
                if (!_blockNumber.isPresent()) {
                    throw new RetryableException("BridgeBalanceState - block number is null. Retrying.");
                }

                String blockNumber = "0x"+Long.toHexString(_blockNumber.get());

                BigInteger _bridgeBalance = api.getBalance(bridgeAddress, blockNumber);
                BigInteger _relayerBalance = api.getBalance(relayerAddress, blockNumber);

                if (_bridgeBalance == null || _relayerBalance == null)
                    throw new RetryableException("BridgeBalanceState - bridgeBalance or relayerBalance returned null");

                // ok to publish to volatile variables
                bridgeBalance = _bridgeBalance;
                relayerBalance = _relayerBalance;

                // try to persist the balances
                if (ds != null) {
                    ds.storeAionEntityBalance(bridgeBalance, relayerBalance, _blockNumber.get());
                }

                // reset error accumulator since all went well
                errAccumulator = 0;
            } catch (QuorumNotAvailableException | RetryableException e) {
                log.debug("BridgeBalanceState - unable to reach quorum or retryable exception caught", e);
                errAccumulator++;
            } catch (InterruptedException e) {
                log.info("BridgeBalanceState - interrupted via InterruptedException #1");
                throw new RuntimeException(e.getCause());
            } catch (PersistenceServiceException e) {
                throw new CriticalBridgeTaskException("AionTipState - unable to update Aion block number");
            }

            try {
                pollIntervalTimeUnit.sleep(pollInterval);
            } catch (InterruptedException e) {
                log.info("BridgeBalanceState - interrupted via InterruptedException #1");
                throw new RuntimeException(e.getCause());
            }
            // all other exceptions, let them just crash the process
        }

        if (errAccumulator >= MAX_CONSECUTIVE_ERR)
            log.debug("BridgeBalanceState - exited due to [errAccumulator >= MAX_CONSECUTIVE_ERR]");

        log.debug("BridgeBalanceState - exiting gracefully ...");
    }

    public void shutdown() {
        this.shutdown = true;
        log.debug("BridgeBalanceState - shutdown signal received");
    }

    private static class RetryableException extends Exception {
        RetryableException(String msg) { super(msg); }
    }
}
