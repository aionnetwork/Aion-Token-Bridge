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

package org.aion.bridge.nexus.workers;

import com.google.common.base.Stopwatch;
import org.aion.bridge.chain.log.LogEnum;
import org.aion.bridge.nexus.AionClient;
import org.aion.bridge.chain.bridge.AionSubmittedTx;
import org.aion.bridge.chain.bridge.CriticalBridgeTaskException;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class TaskQbToQc_BroadcastBundles extends Worker {

    private LinkedBlockingDeque<StatefulBundle> signedBundles;
    private LinkedBlockingDeque<StatefulBundle> submittedBundles;
    private long interval;
    private volatile boolean shutdown = false;
    private int numSlotReserve;
    private AionClient aionClient;
    private final Logger log = LoggerFactory.getLogger(LogEnum.BROADCAST.name());
    private static final long DEFAULT_INTERVAL = 5000;
    private static final int QUEUE_QUERY_INTERVAL = 5;
    private Stopwatch stopWatch;


    public TaskQbToQc_BroadcastBundles(@Nonnull LinkedBlockingDeque<StatefulBundle> signedBundles,
                                       @Nonnull LinkedBlockingDeque<StatefulBundle> submittedBundles,
                                       @Nonnull AionClient aionClient,
                                       long interval, int numSlotReserve) {
        this.signedBundles = signedBundles;
        this.submittedBundles = submittedBundles;
        this.aionClient = aionClient;
        this.interval = interval;
        this.numSlotReserve = numSlotReserve;
        this.stopWatch = Stopwatch.createUnstarted();
    }

    public TaskQbToQc_BroadcastBundles(@Nonnull LinkedBlockingDeque<StatefulBundle> signedBundles,
                                       @Nonnull LinkedBlockingDeque<StatefulBundle> submittedBundles,
                                       @Nonnull AionClient aionClient,
                                       int numSlotReserve) {
        this(signedBundles, submittedBundles, aionClient, DEFAULT_INTERVAL, numSlotReserve);
    }

    @Override
    public void run() {
        while (!shutdown) {

            if (submittedBundles.remainingCapacity() >= numSlotReserve) {
                StatefulBundle toSend = signedBundles.poll();

                if (toSend != null) {

                    log.debug("Sending bundle {} to Aion", toSend.getBundleId());
                    stopWatch.start();
                    AionSubmittedTx txInfo = aionClient.sendAionTransaction(toSend); // AionClient handles resubmission
                    stopWatch.stop();

                    if (txInfo != null) {

                        log.debug("Bundle {} submitted to Aion in {}; Tx: {} in {}", toSend.getBundleId(), toSend.getBundleId(), txInfo.getAionTxHash(), stopWatch.toString());
                        log.trace("Transaction: {} ", txInfo);
                        stopWatch.reset();

                        // Update state and submission info
                        toSend.setSubmitted(txInfo);

                        submittedBundles.offer(toSend);
                        log.info("Bundle {} moving to Submission Q", toSend.getBundleId());

                    } else {
                        // Serious error, already retried 3x

                        log.error("Failed to retrieve txn hash 3 times, BundleId {}, BN {}, ethBlockHash {}, Index {}",
                                toSend.getBundleId(), toSend.getEthBlockNumber(), toSend.getEthBlockHash(), toSend.getIndexInEthBlock());

                        throw new CriticalBridgeTaskException("Failed to retrieve txn hash 3 times\n" + toSend.getErrorString());
                    }
                } else {
                    // No bundles to broadcast, sleep and re-try
                    try {
                        if (!shutdown)
                            TimeUnit.SECONDS.sleep(QUEUE_QUERY_INTERVAL);
                    } catch (InterruptedException e1) {
                        throw new CriticalBridgeTaskException(e1);
                    }
                }
            } else {
                try {
                    if (!shutdown)
                        Thread.sleep(interval);
                } catch (InterruptedException e1) {
                    throw new CriticalBridgeTaskException(e1);
                }
            }

        }
        log.debug("exiting...");
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
    }
}
