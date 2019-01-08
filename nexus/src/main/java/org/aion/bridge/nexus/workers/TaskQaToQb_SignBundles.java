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
import org.aion.bridge.chain.bridge.Signature;
import org.aion.bridge.chain.bridge.CriticalBridgeTaskException;
import org.aion.bridge.chain.log.LogEnum;
import org.aion.bridge.chain.log.LoggerFactory;
import org.aion.bridge.nexus.SignatoryCollector;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.aion.bridge.nexus.retry.Predicates;
import org.aion.bridge.nexus.retry.RetryBuilder;
import org.aion.bridge.nexus.retry.RetryExecutor;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class TaskQaToQb_SignBundles extends Worker {
    private final Logger log = LoggerFactory.getLogger(LogEnum.SIGN.name());
    private SignatoryCollector signatoryCollector;
    private LinkedBlockingDeque<StatefulBundle> bundlesQ;
    private LinkedBlockingDeque<StatefulBundle> signedBundlesQ;
    private long interval;
    private int numSlotReserve;
    private volatile boolean shutdown = false;
    private RetryExecutor<List<Signature>> exec;
    private final static int RETRY_SLEEP_TIME = 10000;
    private static final long DEFAULT_INTERVAL = 5000;
    private static final int DEFAULT_NUM_RETRY = 3;
    private static final int QUEUE_QUERY_INTERVAL = 5;
    private Stopwatch stopWatch;

    @SuppressWarnings("unchecked")
    public TaskQaToQb_SignBundles(LinkedBlockingDeque<StatefulBundle> bundlesQ,
                                  LinkedBlockingDeque<StatefulBundle> signedBundlesQ,
                                  SignatoryCollector signatoryCollector, long interval,
                                  int numRetry, int numSlotReserve) {
        this.signatoryCollector = signatoryCollector;
        this.interval = interval;
        this.bundlesQ = bundlesQ;
        this.signedBundlesQ = signedBundlesQ;
        this.numSlotReserve = numSlotReserve;
        exec = RetryBuilder.newBuilder()
                .retryIf(new Predicates.SignatureFailPredicate())
                .stopAfterAttempt(numRetry)
                .setSleepTime(RETRY_SLEEP_TIME)
                .build();
        stopWatch = Stopwatch.createUnstarted();
    }

    public TaskQaToQb_SignBundles(LinkedBlockingDeque<StatefulBundle> bundlesQ,
                                  LinkedBlockingDeque<StatefulBundle> signedBundlesQ,
                                  SignatoryCollector signatoryCollector,
                                  int numSlotReserve) {
        this(bundlesQ, signedBundlesQ, signatoryCollector, DEFAULT_INTERVAL, DEFAULT_NUM_RETRY, numSlotReserve);
    }

    @Override
    public void run() {
        while (!shutdown) {

            // Ensure QB has enough room to place the signed bundle
            if (signedBundlesQ.remainingCapacity() >= numSlotReserve) {

                StatefulBundle toSign = bundlesQ.poll();

                if (toSign != null) {
                    if (toSign.getState() != StatefulBundle.State.STORED) {

                        log.error("Attempting to sign a bundle outside of bundled state, BundleId {}, State {}, BN {}, ethBlockHash {}",
                                toSign.getBundleId(), toSign.getState(), toSign.getEthBlockNumber(), toSign.getEthBlockHash());

                        throw new CriticalBridgeTaskException("Attempting to sign a bundle outside of bundled state\n" + toSign.getErrorString());
                    }

                    //will either give me the response (set of signatures), or throw an error caught in retry, which leads to null return
                    Callable<List<Signature>> getSignature = () -> signatoryCollector.getSignatureForBundle(toSign);

                    log.trace("Attempting to sign bundle: {}", toSign.getBundleId());

                    stopWatch.start();
                    List<Signature> signatures = exec.execute(getSignature);
                    stopWatch.stop();

                    // if signatures == null, 3 retries have failed. shutdown
                    if (signatures != null) {
                        log.trace("Received signatures for bundle {} : {}", toSign.getBundleId(), signatures.toString());
                        log.debug("Received {} signatures for bundle {} in {}", signatures.size(), toSign.getBundleId(), stopWatch.toString());
                        stopWatch.reset();

                        toSign.setSigned(new HashSet<>(signatures));
                        signedBundlesQ.offer(toSign);
                        log.info("BundleId {} signed, moving to Signed Q", toSign.getBundleId());
                    } else {

                        log.error("Failed to retrieve signature 3 times, BundleId {}, BN {}, EthBlockHash {}, Index {}",
                                toSign.getBundleId(), toSign.getEthBlockNumber(), toSign.getEthBlockHash(), toSign.getIndexInEthBlock());

                        throw new CriticalBridgeTaskException("Failed to retrieve signature 3 times: \n" + toSign.getErrorString());
                    }
                } else {
                    // No bundles to sign, sleep and re-try
                    try {
                        if (!shutdown)
                            TimeUnit.SECONDS.sleep(QUEUE_QUERY_INTERVAL);
                    } catch (InterruptedException e1) {
                        throw new CriticalBridgeTaskException(e1);
                    }
                }

                // Sleep until next round (unless shutdown)
            } else {
                // Sleep till get space in QC
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
