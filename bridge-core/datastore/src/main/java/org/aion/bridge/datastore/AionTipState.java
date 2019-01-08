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
import org.aion.bridge.chain.base.BlockNumberCollector;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.bridge.CriticalBridgeTaskException;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.ObjectUtils.allNotNull;

public class AionTipState extends Thread {

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {

        // Required parameters
        BlockNumberCollector<AionBlock, AionReceipt, AionLog, AionAddress> blockNumberCollector;

        // Optional parameters
        long pollInterval = 10;
        TimeUnit pollIntervalTimeUnit = TimeUnit.SECONDS;
        DataStore ds = null;
        boolean shutdownAfterTipStateError = true;

        public Builder setPollInterval(long pollInterval, TimeUnit timeUnit) {
            this.pollInterval = pollInterval;
            this.pollIntervalTimeUnit = timeUnit;
            return this;
        }

        public Builder setApi(BlockNumberCollector<AionBlock, AionReceipt, AionLog, AionAddress> x) { blockNumberCollector = x;return this; }

        public Builder setDatabase(DataStore x) { ds = x;return this; }

        public Builder setShutdownAfterTipStateError(boolean x) { shutdownAfterTipStateError = x;return this; }


        public AionTipState build() {
            if (!allNotNull(blockNumberCollector))
                throw new IllegalArgumentException("blockNumberCollector mut be non-null");

            return new AionTipState(this);
        }
    }

    private final Logger log = LoggerFactory.getLogger(AionTipState.class);

    private volatile Long blockNumber = null;
    private volatile boolean shutdown = false;

    private BlockNumberCollector<AionBlock, AionReceipt, AionLog, AionAddress> blockNumberCollector;
    private final long pollInterval;
    private final TimeUnit pollIntervalTimeUnit;
    private DataStore ds;
    boolean shutdownAfterTipStateError;

    private static final int MAX_CONSECUTIVE_ERR = 5;

    public AionTipState(Builder b) {
        this.pollInterval = b.pollInterval;
        this.pollIntervalTimeUnit = b.pollIntervalTimeUnit;
        this.shutdownAfterTipStateError = b.shutdownAfterTipStateError;
        this.blockNumberCollector = b.blockNumberCollector;
        this.ds = b.ds;
    }

    public Optional<Long> getBlockNumber() {
        return Optional.ofNullable(blockNumber);
    }

    @Override
    public void run() {
        int errAccumulator = 0;
        while (!shutdown && errAccumulator < MAX_CONSECUTIVE_ERR) {
            try {
                Optional<Long> blockNumber = blockNumberCollector.getLatestBlockNumber();
                if (!blockNumber.isPresent()) {
                    throw new RetryableException("Block number cannot be retrieved. Retrying");
                }

                // set the volatile accessor
                this.blockNumber = blockNumber.get();

                // write to disk if all went well
                if (ds != null)
                    ds.storeAionLatestBlock(blockNumber.get());

                errAccumulator = 0; // since all went well
            } catch (QuorumNotAvailableException | RetryableException e) {
                log.debug("Unable to reach quorum", e);
                if (shutdownAfterTipStateError)
                    errAccumulator++;
            } catch (InterruptedException e) {
                log.info("Interrupted via InterruptedException #1");
                throw new RuntimeException(e.getCause());
            } catch (PersistenceServiceException e) {
                throw new CriticalBridgeTaskException("Caught a PersistenceServiceException. Crashing the process.");
            }

            try {
                pollIntervalTimeUnit.sleep(pollInterval);
                //TimeUnit.MILLISECONDS.sleep(pollInterval);
            } catch (InterruptedException e) {
                log.info("Interrupted via InterruptedException");
                throw new RuntimeException(e.getCause());
            }
            // all other exceptions, let them just crash the process
        }

        if (errAccumulator >= MAX_CONSECUTIVE_ERR) {
            log.debug("Exited due to [errAccumulator >= MAX_CONSECUTIVE_ERR]");
            throw new CriticalBridgeTaskException("Exited due to errAccumulator >= MAX_CONSECUTIVE_ERR.");
        }

        log.debug("Exiting gracefully ...");
    }

    public void shutdown() {
        this.shutdown = true;
        log.debug("Shutdown signal received");
    }

    private static class RetryableException extends Exception {
        RetryableException(String msg) {
            super(msg);
        }
    }
}


