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
import org.aion.bridge.chain.aion.rpc.FvmAbiCodec;
import org.aion.bridge.chain.aion.rpc.abi.FvmBytes32;
import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.aion.types.AionEventFilter;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.aion.types.BlakeBloom;
import org.aion.bridge.chain.base.api.IncompleteApiCallException;
import org.aion.bridge.chain.base.api.MalformedApiResponseException;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.AionBundle;
import org.aion.bridge.chain.bridge.AionUnbundlingPolicy;
import org.aion.bridge.chain.log.LogEnum;
import org.aion.bridge.datastore.AionTipState;
import org.aion.bridge.chain.bridge.CriticalBridgeTaskException;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.aion.bridge.nexus.retry.Predicates;
import org.aion.bridge.nexus.retry.RetryBuilder;
import org.aion.bridge.nexus.retry.RetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

public class TaskQcToQd_CollectReceipts extends Worker {

    private AionTipState aionTipState;
    private LinkedBlockingDeque<StatefulBundle> submittedQ;
    private LinkedBlockingDeque<StatefulBundle> finalizationQ;
    private final int minDepth;
    private int numRetry;
    private AionJsonRpcConsolidator aionConsolidator;

    private final long MS_DELAY_POLL = 5_000L;
    private static final int RETRY_SLEEP_TIME = 50000;
    private static final AionAddress CONTRACT_ADDRESS = new AionAddress("0000000000000000000000000000000000000000000000000000000000000200");
    private static final int DEFAULT_NUM_RETRY = 3;
    private AionEventFilter successfulTxhashFilter;
    private static final int SUCCESSFUL_TXHASH_TOPICS_COUNT = 2;
    private AionUnbundlingPolicy unbundlingPolicy;
    private volatile boolean shutdown = false;
    private Stopwatch stopWatch;

    RetryExecutor<AionReceipt> exec;

    private static final Logger log = LoggerFactory.getLogger(LogEnum.INIT_VERIFY.name());

    @SuppressWarnings("unchecked")
    public TaskQcToQd_CollectReceipts(@Nonnull LinkedBlockingDeque<StatefulBundle> submittedQ,
                                      @Nonnull LinkedBlockingDeque<StatefulBundle> finalizationQ,
                                      @Nonnull AionJsonRpcConsolidator aionConsolidator,
                                      @Nonnull AionTipState aionTipState,
                                      @Nonnull int minDepth,
                                      @Nonnull int numRetry,
                                      @Nonnull String successfulTxHashEvent,
                                      @Nonnull AionUnbundlingPolicy unbundlingPolicy) {

        this.submittedQ = submittedQ;
        this.finalizationQ = finalizationQ;
        this.aionConsolidator = aionConsolidator;
        this.aionTipState = aionTipState;
        this.numRetry = numRetry;
        this.minDepth = minDepth;
        this.successfulTxhashFilter = new AionEventFilter(CONTRACT_ADDRESS, successfulTxHashEvent, new BlakeBloom());
        this.unbundlingPolicy = unbundlingPolicy;

        exec = RetryBuilder.newBuilder()
                .retryIf(new Predicates.ReceiptFailPredicate())
                .stopAfterAttempt(this.numRetry)
                .setSleepTime(RETRY_SLEEP_TIME)
                .build();
        stopWatch = Stopwatch.createUnstarted();
    }

    public TaskQcToQd_CollectReceipts(@Nonnull LinkedBlockingDeque<StatefulBundle> submittedQ,
                                      @Nonnull LinkedBlockingDeque<StatefulBundle> finalizationQ,
                                      @Nonnull AionJsonRpcConsolidator aionConsolidator,
                                      @Nonnull AionTipState aionTipState,
                                      int minDepth,
                                      @Nonnull String successfulTxHashEvent,
                                      @Nonnull AionUnbundlingPolicy unbundlingPolicy) {
        this(submittedQ, finalizationQ, aionConsolidator, aionTipState, minDepth, DEFAULT_NUM_RETRY, successfulTxHashEvent, unbundlingPolicy);
    }

    @Override
    public void run() {

        while (!shutdown) {

            if (finalizationQ.remainingCapacity() >= 1) {
                try {
                    StatefulBundle bundle = submittedQ.peek();

                    Optional<Long> blockNumber = aionTipState.getBlockNumber();

                    if (!blockNumber.isPresent()) {
                        log.debug("Block number reported by Aion Tip State is null. Sleeping and trying again.");
                        Thread.sleep(MS_DELAY_POLL);
                        continue;
                    }

                    if (bundle == null) {
                        log.debug("No bundles awaiting finalization, sleep and retry");
                        Thread.sleep(MS_DELAY_POLL);
                        continue;
                    } else if ((blockNumber.get() - bundle.getTxSubmitted().getAionBlockNumber()) < minDepth) {
                        log.trace("Bundle {} has not reached initial finalization depth {}, sleeping and retrying", bundle.getBundleId(), minDepth);
                        Thread.sleep(MS_DELAY_POLL);
                        continue;
                    }

                    stopWatch.start();
                    AionReceipt receipt = getReceipt(bundle.getTxSubmitted().getAionTxHash());
                    log.trace("Retrieved Aion receipt {} in {}", receipt, stopWatch.toString());
                    stopWatch.stop();
                    stopWatch.reset();

                    if (receipt == null) {
                        try {
                            // check if bundle was included before. update txn hash if it was, otherwise it's a critical error
                            Word32 transactionHash = isBundleIncluded(bundle.getBundleHash());
                            if (transactionHash.equals(Word32.EMPTY)) {

                                log.error("Failed to retrieve the receipt 3 times, shutting down. BN: {}, Idx: {}, aionTx: {}, " +
                                                "transferSize: {}", bundle.getEthBlockNumber(), bundle.getIndexInEthBlock(),
                                        bundle.getTxSubmitted().getAionTxHash(),
                                        bundle.getTransfers().size());

                                throw new CriticalBridgeTaskException("Failed to retrieve the receipt 3 times: \n" + bundle.getErrorString());
                            } else {
                                log.info("Fetching receipt for BundleId {}, AionTx {} failed, but bundle was already included.",
                                        bundle.getBundleId(), bundle.getTxSubmitted().getAionTxHash().toStringWithPrefix());
                                log.info("Updating txn hash to {} for BundleId {}", transactionHash.toStringWithPrefix(), bundle.getBundleId());

                                bundle.getTxSubmitted().setAionTxHash(transactionHash);
                                continue;
                            }
                        } catch (IncompleteApiCallException | QuorumNotAvailableException | MalformedApiResponseException e) {
                            throw new CriticalBridgeTaskException("Failed to retrieve the receipt 3 times: \n" + bundle.getErrorString());
                        }
                    }

                    if (blockNumber.get() - receipt.getBlockNumber() < minDepth) {
                        log.debug("Bundle {} has not reached init finalization, current {}, target {}", bundle.getBundleId(), blockNumber.get() - receipt.getBlockNumber(), minDepth);
                        Thread.sleep(MS_DELAY_POLL);
                        continue;
                    }

                    submittedQ.poll();

                    // receipt with a failed status
                    if (!receipt.getStatus()) {
                        log.error("Error in contract caused receipt failure, shutting down. BN: {}, Idx: {}, aionTx: {}, " +
                                        "transferSize: {}, logSize: {}", bundle.getEthBlockNumber(),
                                bundle.getIndexInEthBlock(), bundle.getTxSubmitted().getAionTxHash(),
                                bundle.getTransfers().size(), receipt.getEventLogs().size());

                        throw new CriticalBridgeTaskException("Error in contract caused receipt failure: \n" + bundle.getErrorString());
                    }

                    //check if it's a new distribution event and  all transfers are included in the receipt
                    if (receipt.getEventLogs().size() == (bundle.getTransfers().size() + 1)) {

                        List<AionBundle> filteredBundle = unbundlingPolicy.fromFilteredReceipt(Arrays.asList(receipt));

                        if (filteredBundle.size() == 1 &&
                                filteredBundle.get(0).getTransfers().size() == bundle.getTransfers().size() &&
                                filteredBundle.get(0).getTransfers().containsAll(bundle.getTransfers())) {
                            //success
                            log.debug("Successfully submitted bundle {} to Aion in Tx {}, block # {}", bundle.getBundleId(), receipt.getTransactionHash(), receipt.getBlockNumber());
                            bundle.setSealed(receipt);

                            finalizationQ.offer(bundle);
                            log.info("Moving Bundle {} to Finalization Q", bundle.getBundleId());

                        } else {
                            throw new CriticalBridgeTaskException("Aion bundle receipt logs and eth transfers do not match." + bundle.getErrorString() + "EventLogSize: " + receipt.getEventLogs().size());
                        }
                        //if bundle was included, a new event is generated
                    } else if (successfulTxhashFilter.matches(receipt.getLogsBloom()) &&
                            receipt.getEventLogs().get(0).getTopics().size() == SUCCESSFUL_TXHASH_TOPICS_COUNT &&
                            receipt.getEventLogs().get(0).getTopics().get(0).equals(successfulTxhashFilter.getEventHash()) &&
                            receipt.getTo().equals(CONTRACT_ADDRESS)) {

                        Word32 txnHash = receipt.getEventLogs().get(0).getTopics().get(1);
                        bundle.getTxSubmitted().setAionTxHash(txnHash);
                        log.debug("Bundle {} was included in a previous transaction, fetching original Tx", bundle.getBundleId());

                        // since it was before the current transaction, it is finalized
                        AionReceipt originalReceipt = getReceipt(txnHash);
                        if (originalReceipt == null) {
                            log.error("Failed to retrieve receipt for included bundle 3 times, shutting down. BN: {}, Idx: {}, aionTx: {}, " +
                                            "transferSize: {}", bundle.getEthBlockNumber(), bundle.getIndexInEthBlock(),
                                    bundle.getTxSubmitted().getAionTxHash(), bundle.getTransfers().size());

                            throw new CriticalBridgeTaskException("Failed to retrieve receipt for included bundle 3 times" + bundle.getErrorString());
                        }

                        log.debug("Bundle {} was included in a previous transaction, original tx: {}", bundle.getBundleId(), originalReceipt.getTransactionHash());

                        bundle.setSealed(originalReceipt);
                        finalizationQ.offer(bundle);
                        log.info("Moving Bundle {} to Finalization Q", bundle.getBundleId());

                    } else {
                        //fail
                        log.error("Number of logs and bundle transfers don't match, shutting down. EthBN: {}, Idx: {}, aionTx: {}, " +
                                        "transferSize: {}, logSize: {}. Expected {} logs", bundle.getEthBlockNumber(), bundle.getIndexInEthBlock(), bundle.getTxSubmitted().getAionTxHash(),
                                bundle.getTransfers().size(), receipt.getEventLogs().size(), bundle.getTransfers().size());

                        throw new CriticalBridgeTaskException("Number of logs and bundle transfers don't match \n" + bundle.getErrorString() + "EventLogSize: " + receipt.getEventLogs().size());
                    }

                } catch (InterruptedException e) {
                    shutdown = true;
                    throw new CriticalBridgeTaskException(e);
                }
            } else {
                try {
                    if (!shutdown)
                        Thread.sleep(MS_DELAY_POLL);
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

    private Word32 isBundleIncluded(Word32 bundleHash) throws MalformedApiResponseException, IncompleteApiCallException, InterruptedException, QuorumNotAvailableException {

        ImmutableBytes txnData = new ImmutableBytes(
                new FvmAbiCodec("actionMap(bytes32)",
                        new FvmBytes32(bundleHash))
                        .encode());
        return new Word32(aionConsolidator.getApi().contractCall(CONTRACT_ADDRESS, txnData));
    }

    private AionReceipt getReceipt(Word32 txnHash) {
        //Execution exception: Null Pointer
        Callable<AionReceipt> getReceipt = () -> {
            // Returns null in case of error/if not found
            return aionConsolidator.getApi().getReceipt(txnHash).orElse(null);
        };
        return exec.execute(getReceipt);
    }
}



