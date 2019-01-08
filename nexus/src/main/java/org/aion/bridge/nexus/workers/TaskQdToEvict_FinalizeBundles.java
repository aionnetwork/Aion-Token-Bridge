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
import org.aion.bridge.chain.aion.api.AionJsonRpcConsolidator;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.AionBundle;
import org.aion.bridge.chain.bridge.AionUnbundlingPolicy;
import org.aion.bridge.chain.bridge.CriticalBridgeTaskException;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.log.LogEnum;
import org.aion.bridge.datastore.AionTipState;
import org.aion.bridge.datastore.DataStore;
import org.aion.bridge.nexus.retry.Predicates;
import org.aion.bridge.nexus.retry.RetryBuilder;
import org.aion.bridge.nexus.retry.RetryExecutor;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

public class TaskQdToEvict_FinalizeBundles extends Worker {
    private AionTipState tipState;
    private LinkedBlockingDeque<StatefulBundle> finalizationQ;
    private final int finalizationDepth;
    private AionJsonRpcConsolidator consolidator;
    private static final long MS_DELAY_POLL = 5_000L;
    private static final int MS_RETRY_RECEIPT_QUERY = 30000;
    private AionUnbundlingPolicy unbundlingPolicy;
    private RetryExecutor<AionReceipt> exec;
    private static final int DEFAULT_NUM_RETRY = 3;

    private static final Logger log = LoggerFactory.getLogger(LogEnum.FINALIZE.name());

    private DataStore ds;

    private Stopwatch stopWatch;

    public TaskQdToEvict_FinalizeBundles(@Nonnull DataStore ds,
                                         @Nonnull LinkedBlockingDeque<StatefulBundle> finalizationQ,
                                         @Nonnull AionTipState tipState,
                                         @Nonnull AionJsonRpcConsolidator consolidator,
                                         int finalizationDepth,
                                         @Nonnull AionUnbundlingPolicy unbundlingPolicy) {
        this.tipState = tipState;
        this.finalizationQ = finalizationQ;
        this.consolidator = consolidator;
        this.ds = ds;
        this.finalizationDepth = finalizationDepth;
        this.unbundlingPolicy = unbundlingPolicy;

        this.exec = RetryBuilder.newBuilder()
                .retryIf(new Predicates.ReceiptFailPredicate())
                .stopAfterAttempt(this.DEFAULT_NUM_RETRY)
                .setSleepTime(MS_RETRY_RECEIPT_QUERY)
                .build();
        stopWatch = Stopwatch.createUnstarted();
    }

    public TaskQdToEvict_FinalizeBundles(@Nonnull LinkedBlockingDeque<StatefulBundle> finalizationQ,
                                         @Nonnull AionTipState tipState,
                                         @Nonnull AionJsonRpcConsolidator consolidator,
                                         int finalizationDepth,
                                         @Nonnull AionUnbundlingPolicy unbundlingPolicy) {
        this.tipState = tipState;
        this.finalizationQ = finalizationQ;
        this.consolidator = consolidator;
        this.finalizationDepth = finalizationDepth;
        this.unbundlingPolicy = unbundlingPolicy;
        this.exec = RetryBuilder.newBuilder()
                .retryIf(new Predicates.ReceiptFailPredicate())
                .stopAfterAttempt(this.DEFAULT_NUM_RETRY)
                .setSleepTime(MS_RETRY_RECEIPT_QUERY)
                .build();
        stopWatch = Stopwatch.createUnstarted();
    }

    private volatile boolean shutdown = false;

    @Override
    public void shutdown() {
        this.shutdown = true;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                List<StatefulBundle> finalizedBundles = findFinalizedBundles();

                //Submit all finalized bundles to DB
                if (!finalizedBundles.isEmpty() && ds != null) {
                    stopWatch.start();
                    ds.storeAionFinalizedBundles(finalizedBundles,
                            new ChainLink(finalizedBundles.get(finalizedBundles.size() - 1).getAionReceipt().getBlockNumber(),
                                    finalizedBundles.get(finalizedBundles.size() - 1).getAionReceipt().getBlockHash()),
                            finalizedBundles.get(finalizedBundles.size() - 1));
                    stopWatch.stop();
                    log.trace("Stored {} bundles in DB within {}", finalizedBundles.size(), stopWatch.toString());
                    stopWatch.reset();
                }

                for (StatefulBundle fb : finalizedBundles) {
                    long db = ds.getEthBundleCreationTimestamp(fb.getBundleId()).get().getTime();
                    //long dbInst = db.toInstant().toEpochMilli();
                    long now = Instant.now().toEpochMilli();

                    Duration interval = Duration.ofMillis(now - db);

                    if(interval.isNegative())
                        log.debug("Detected negative interval for bundle {} ", fb.getBundleId());

                    log.debug("Completed transferring bundle: {}, total transfer time: {}", fb.getBundleId(),
                                DurationFormatUtils.formatDuration(Math.max(interval.toMillis(), 0L),"HH:mm:ss:SSS")
                    );
                }

                //Sleep until next iteration
                Thread.sleep(MS_DELAY_POLL);

            } catch (InterruptedException e) {
                shutdown = true;
                log.debug("Interrupted");
                //throw new CriticalBridgeTaskException(e);
            } catch (PersistenceServiceException e) {
                log.error("TaskQdToEvict_FinalizeBundles - caught a PersistenceServiceException. Crashing the process.");
                throw new CriticalBridgeTaskException(e);
            }
        }

        log.debug("exiting...");
    }

    private List<StatefulBundle> findFinalizedBundles() {
        List<StatefulBundle> finalized = new ArrayList<>();

        // Gather all receipts from the finalization Q which have reached the correct depth
        while (!shutdown) {
            StatefulBundle bundle = finalizationQ.peek();

            if (bundle == null) {
                log.trace("No bundles present in finalization queue");
                break;
            }

            Optional<Long> blockNumber = tipState.getBlockNumber();
            if (!blockNumber.isPresent()) {
                log.debug("Block number reported by Aion Tip State is null. Will sleep and retry.");
                break;
            }

            stopWatch.start();
            AionReceipt receipt = getReceipt(bundle.getTxSubmitted().getAionTxHash());
            stopWatch.stop();
            log.debug("Retrieved receipt for bundle {} in {}", bundle.getBundleId(), stopWatch.toString());
            stopWatch.reset();

            if (receipt == null) {
                log.error("Failed to retrieve the receipt 3 times - possible deep re-org, shutting down. BN: {}, Idx: {}, aionTx: {}, " +
                                "transferSize: {}", bundle.getEthBlockNumber(), bundle.getIndexInEthBlock(),
                        bundle.getTxSubmitted().getAionTxHash(),
                        bundle.getTransfers().size());

                throw new CriticalBridgeTaskException("Failed to retrieve the receipt 3 times - possible deep re-org: \n" + bundle.getErrorString());
            }

            // Head - receiptDepth
            long bundleDepth = blockNumber.get() - receipt.getBlockNumber();


            if (bundleDepth < 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Bundle depth < 0 detected: \n");
                sb.append("Tipstate BN: ");
                sb.append(blockNumber.get());
                sb.append("\n");
                sb.append("Receipt BN: ");
                sb.append(receipt.getBlockNumber());
                sb.append("\n");

                log.error("Bundle depth < 0 detected for bundle {}, tipState {}, Receipt BN: {}", bundle.getBundleId(), blockNumber.get(), receipt.getBlockNumber());

                throw new CriticalBridgeTaskException(sb.toString());
            }

            if (bundleDepth < finalizationDepth)
                break;

            log.debug("Found bundle which has reached finalization depth; bundle: {}", bundle.getBundleId());

            // Check if found receipt has changed from the originally found receipt (re-org detected - not a critical error)
            if (!receipt.equals(bundle.getAionReceipt())) {
                log.info("Non-critical error: Originally found receipt does not match found receipt; " +
                                "original blockNum: {}, original blockHash: {}," +
                                "found blockNum: {}, found blockHash: {}",
                        bundle.getAionReceipt().getBlockNumber(), bundle.getAionReceipt().getBlockHash(),
                        receipt.getBlockNumber(), receipt.getBlockHash());
                bundle.resetSealed(receipt);
            }

            if (!receipt.getStatus()) {
                log.error("Aion bundle receipt indicates failure, shutting down. " +
                                "BN: {}, Idx: {}, aionTx: {}, transfer size: {}",
                        bundle.getBundle().getEthBlockNumber(), bundle.getBundle().getIndexInEthBlock(), bundle.getTxSubmitted().getAionTxHash(),
                        bundle.getTransfers().size());

                throw new CriticalBridgeTaskException("Aion bundle receipt indicates failure" + bundle.getErrorString());
            }

            // Check bundle validation in unbundling policy before finalizing blocks
            List<AionBundle> filteredBundle = unbundlingPolicy.fromFilteredReceipt(Arrays.asList(bundle.getAionReceipt()));
            if (filteredBundle.size() == 1 &&
                    filteredBundle.get(0).getTransfers().size() == bundle.getTransfers().size() &&
                    filteredBundle.get(0).getTransfers().containsAll(bundle.getTransfers())) {
                log.debug("Bundle: {} verified and evicted, awaiting DB update before finalization", bundle.getBundleId());

                // Remove bundle from Queue
                finalizationQ.poll();

                //Finalize
                bundle.setFinalized();

                finalized.add(bundle);
            } else {
                log.error("Aion bundle receipt logs and eth transfers do not match, shutting down. " +
                                "BN: {}, Idx: {}, aionTx: {}, transfer size: {}",
                        bundle.getBundle().getEthBlockNumber(), bundle.getBundle().getIndexInEthBlock(), bundle.getTxSubmitted().getAionTxHash(),
                        bundle.getTransfers().size());

                throw new CriticalBridgeTaskException("Aion bundle receipt logs and eth transfers do not match" + bundle.getErrorString());
            }
        }
        return finalized;
    }

    private AionReceipt getReceipt(Word32 txnHash) {
        //Execution exception: Null Pointer
        Callable<AionReceipt> getReceipt = () -> {
            // Returns null in case of error/if not found
            return consolidator.getApi().getReceipt(txnHash).orElse(null);
        };
        return exec.execute(getReceipt);
    }
}
