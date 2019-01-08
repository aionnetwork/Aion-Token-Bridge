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

import com.google.common.collect.Queues;
import org.aion.bridge.chain.aion.api.AionJsonRpcConsolidator;
import org.aion.bridge.chain.base.oracle.ChainHistory;
import org.aion.bridge.chain.base.oracle.ChainOracle;
import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.bridge.AionUnbundlingPolicy;
import org.aion.bridge.chain.bridge.EthBundlingPolicy;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.eth.api.EthJsonRpcConsolidator;
import org.aion.bridge.chain.log.LogEnum;
import org.aion.bridge.chain.log.LoggingSetup;
import org.aion.bridge.datastore.AionTipState;
import org.aion.bridge.datastore.DataStore;
import org.aion.bridge.datastore.DbConnectionManager;
import org.aion.bridge.chain.bridge.PersistentBundle;
import org.aion.bridge.nexus.workers.TaskQaToQb_SignBundles;
import org.aion.bridge.nexus.workers.TaskQbToQc_BroadcastBundles;
import org.aion.bridge.nexus.workers.TaskQcToQd_CollectReceipts;
import org.aion.bridge.nexus.workers.TaskQdToEvict_FinalizeBundles;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.commons.lang3.ObjectUtils.allNotNull;


public class Bridge {

    private static final Logger log = LoggerFactory.getLogger(Bridge.class);

    // Queue sizes
    private static final int QA_SIZE = 5120; //~100MB
    private static final int QB_SIZE = 5120; //~100MB
    private static final int QC_SIZE = 25600; // ~500MB
    private static final int QD_SIZE = 25600; // ~500MB

    private final int numThreadCtoD = 1; //Cannot run multithreaded in current impl
    private final int numThreadDtoE = 1; //Cannot run multithreaded in current impl

    private static final long SHUTDOWN_LIMIT = 3000;

    @GuardedBy("shutdownLock")
    private Boolean isShuttingDown = false;

    private volatile Lock shutdownLock = new ReentrantLock();

    // Bundles extracted from eth blocks
    private LinkedBlockingDeque<StatefulBundle> QA_Bundles = Queues.newLinkedBlockingDeque(QA_SIZE);
    // Bundles which have been signed by signatories
    private LinkedBlockingDeque<StatefulBundle> QB_SignedBundles = Queues.newLinkedBlockingDeque(QB_SIZE);
    // Bundles which have been submitted to Aion (Contain Tx hash)
    private LinkedBlockingDeque<StatefulBundle> QC_AwaitingReceiptBundles = Queues.newLinkedBlockingDeque(QC_SIZE);
    // Bundles which have had their receipts retrieved (Contains aion receipt)
    private LinkedBlockingDeque<StatefulBundle> QD_AwaitingFinalizationBundles = Queues.newLinkedBlockingDeque(QD_SIZE);

    private SignatoryCollector signatoryCollector;
    private AionClient aionClient;
    private AionJsonRpcConsolidator aionJsonRpcConsolidator;
    private EthJsonRpcConsolidator ethJsonRpcConsolidator;

    private AionTipState tipState;

    private int aionFinalizationLimit;
    private int receiptMinDepth;
    // List of threads
    private List<TaskQaToQb_SignBundles> signatoryClientThreads;
    private List<TaskQbToQc_BroadcastBundles> submissionThreads;
    private List<TaskQcToQd_CollectReceipts> receiptCollectorThreads;
    private List<TaskQdToEvict_FinalizeBundles> finalizeBundleThreads;

    private ChainHistory chainHistory;
    private ChainOracle chainOracle;
    private AionUnbundlingPolicy unbundlingPolicy;

    private DataStore dataStore;
    private DbConnectionManager dbConnectionManager;
    private String successfulTxHashEvent;

    // Status thread
    private QueueStatusThread queueStatusThread;

    private ThreadPoolExecutor executor;

    private Bridge(Builder b){
        this.dataStore = b.dataStore;
        this.signatoryCollector = b.signatoryCollector;
        this.aionClient = b.aionClient;
        this.aionJsonRpcConsolidator = b.aionJsonRpcConsolidator;
        this.tipState = b.aionTipState;
        this.aionFinalizationLimit = b.aionFinalizationLimit;
        this.receiptMinDepth = b.receiptMinDepth;
        this.unbundlingPolicy = b.unbundlingPolicy;
        this.dbConnectionManager = b.dbConnectionManager;
        this.successfulTxHashEvent = b.successfulTxHashEvent;
        this.ethJsonRpcConsolidator = b.ethJsonRpcConsolidator;
        this.executor = b.executor;
        signatoryClientThreads = new ArrayList<>();
        submissionThreads = new ArrayList<>();
        receiptCollectorThreads = new ArrayList<>();
        finalizeBundleThreads = new ArrayList<>();

        if(dataStore == null)
            chainHistory = new NonPersistentChainHistory(QA_Bundles, b.startBlock, b.ethBundlingPolicy);
        else
            chainHistory = new PersistentChainHistory(dataStore, QA_Bundles, b.startBlock, b.ethBundlingPolicy);
    }

    public void initializeThreads(int numThreadAtoB, int numThreadBtoC) {

        for (int i = 0; i < numThreadAtoB; i++) {
            TaskQaToQb_SignBundles signBundles = new TaskQaToQb_SignBundles(QA_Bundles, QB_SignedBundles,
                    this.signatoryCollector, numThreadAtoB);
            signBundles.setName("QAtoQB_" + i);
            signBundles.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            signatoryClientThreads.add(signBundles);
        }

        for (int i = 0; i < numThreadBtoC; i++) {
            TaskQbToQc_BroadcastBundles submitBundles = new TaskQbToQc_BroadcastBundles(QB_SignedBundles,
                    QC_AwaitingReceiptBundles, aionClient, numThreadBtoC);

            submitBundles.setName("QBtoQC_" + i);
            submitBundles.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            submissionThreads.add(submitBundles);
        }

        for (int i = 0; i < numThreadCtoD; i++) {
            TaskQcToQd_CollectReceipts receiptCollector;

            //if (dataStore == null || dbConnectionManager == null) {
            receiptCollector = new TaskQcToQd_CollectReceipts(QC_AwaitingReceiptBundles,
                    QD_AwaitingFinalizationBundles,
                    aionJsonRpcConsolidator,
                    tipState,
                    receiptMinDepth,
                    successfulTxHashEvent,
                    unbundlingPolicy);
            receiptCollector.setName("QCtoQD_" + i);
            receiptCollector.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            receiptCollectorThreads.add(receiptCollector);
        }

        for (int i = 0; i < numThreadDtoE; i++) {

            TaskQdToEvict_FinalizeBundles finalizeBundles;
            if (dataStore == null) {
                finalizeBundles = new TaskQdToEvict_FinalizeBundles(QD_AwaitingFinalizationBundles,
                        tipState,
                        aionJsonRpcConsolidator,
                        aionFinalizationLimit,
                        unbundlingPolicy);
            } else {
                finalizeBundles = new TaskQdToEvict_FinalizeBundles(dataStore,
                        QD_AwaitingFinalizationBundles,
                        tipState,
                        aionJsonRpcConsolidator,
                        aionFinalizationLimit,
                        unbundlingPolicy);
            }
            // Queue D to Eviction (Finalize bundles)
            finalizeBundles.setName("QDtoE_" + i);
            finalizeBundles.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            finalizeBundleThreads.add(finalizeBundles);
        }


        chainOracle.setName("OtoA");
        chainOracle.setUncaughtExceptionHandler(new CriticalExceptionHandler());
        tipState.setUncaughtExceptionHandler(new CriticalExceptionHandler());

        queueStatusThread = new QueueStatusThread();

    }

    private void initializeQueues() throws PersistenceServiceException, InterruptedException {
        Optional<Long> ethFinalized = dataStore.getEthFinalizedBundleId();
        Optional<Long> aionFinalized = dataStore.getAionFinalizedBundleId();

        // No finalized eth or aion bundles present
        if(!ethFinalized.isPresent() && !aionFinalized.isPresent())
            return;

        Long start;
        Long end;

        if(ethFinalized.isPresent() && !aionFinalized.isPresent()) {
            // Eth present but no aion finalized
            start = 0L;
            end = ethFinalized.get();
        } else {
            // Both Eth and Aion bundles finalized present in DB

            // Bundles are equal, nothing outstanding to populate
            if(ethFinalized.get().equals(aionFinalized.get()))
                return;

            // Some difference present between finalized bundles on eth and finalized on aion
            start = aionFinalized.get() + 1;
            end = ethFinalized.get();
        }

        if (end < start)
            throw new IllegalStateException("Inconsistent DB state; ethFinalized cannot be less than aionFinalized");

        if (end - start >= 0) {
            Optional<List<PersistentBundle>> storedBundles = dataStore.getBundleRangeClosed(start, end);

            if (storedBundles.isPresent()) {
                for (PersistentBundle b : storedBundles.get()) {
                    // Re-create bundles to the state of Stored as it came from the DB
                    StatefulBundle sb = new StatefulBundle(b);
                    sb.setStored();

                    log.info("Populating QA for bundle {}, ethTxHash {}", sb.getBundleId(), sb.getEthBlockHash());

                    // Redesign to gradually populate queue from DB instead of sending directly??
                    while(!QA_Bundles.offer(sb)) {
                        TimeUnit.SECONDS.sleep(5);
                    }
                }
            }
        }
    }

    public ChainHistory getChainHistory() {
        return this.chainHistory;
    }

    public void setBridgeOracle(ChainOracle chainOracle) {
        this.chainOracle = chainOracle;
    }

    public void start() throws InterruptedException, PersistenceServiceException {
        log.info("Starting Main event processor");

        queueStatusThread.start();

        tipState.start();

        for (TaskQaToQb_SignBundles task : signatoryClientThreads)
            task.start();

        for (TaskQbToQc_BroadcastBundles task : submissionThreads)
            task.start();

        for (TaskQcToQd_CollectReceipts task : receiptCollectorThreads)
            task.start();

        for (TaskQdToEvict_FinalizeBundles task : finalizeBundleThreads)
            task.start();

        // Populate queue with outstanding transfers
        if (dataStore != null)
            initializeQueues();

        chainOracle.start();
    }

    @GuardedBy("shutdownLock")
    public void shutdown() {
        //TODO: shutdown threads once decide on what threads server will manage
        this.shutdownLock.lock();
        try {
            // indicates that a shutdown has already started, this can happen
            // if another thread calls shutdown (for example Ctrl+C runtime thread)
            // otherwise, this can be called from the main thread via UncaughtExceptionHandlers
            if (this.isShuttingDown)
                return;
            this.isShuttingDown = true;
        } finally {
            this.shutdownLock.unlock();
        }


        log.info("Shutting down ChainOracle");
        chainOracle.shutdown();

        log.info("Shutting down AionTipState");
        tipState.shutdown();

        log.info("Shutting down TaskQaToQb");
        for (TaskQaToQb_SignBundles task : signatoryClientThreads)
            task.shutdown();

        log.info("Shutting down TaskQbToQc");
        for (TaskQbToQc_BroadcastBundles task : submissionThreads)
            task.shutdown();

        log.info("Shutting down TaskQcToQd");
        for (TaskQcToQd_CollectReceipts task : receiptCollectorThreads)
            task.shutdown();

        log.info("Shutting down TaskQdtoE");
        for (TaskQdToEvict_FinalizeBundles task : finalizeBundleThreads)
            task.shutdown();

        try {
            chainOracle.join(SHUTDOWN_LIMIT);
        } catch (InterruptedException e) {
            log.debug("Bridge - chainOracle interrupted", e);
        }

        try {
            tipState.join(SHUTDOWN_LIMIT);
        } catch (InterruptedException e) {
            log.debug("Bridge - tipState interrupted", e);
        }

        for (TaskQaToQb_SignBundles task : signatoryClientThreads)
        {
            try {
                task.join(SHUTDOWN_LIMIT);
            } catch (InterruptedException e1) {
                log.debug("Bridge - signatoryClientThreads interrupted", e1);
            }
        }

        for (TaskQbToQc_BroadcastBundles task : submissionThreads)
        {
            try {
                task.join(SHUTDOWN_LIMIT);
            } catch (InterruptedException e1) {
                log.debug("Bridge - submissionThreads interrupted", e1);
            }
        }

        for (TaskQcToQd_CollectReceipts task : receiptCollectorThreads)
        {
            try {
                task.join(SHUTDOWN_LIMIT);
            } catch (InterruptedException e1) {
                log.debug("Bridge - receiptCollectorThreads interrupted", e1);
            }
        }

        for (TaskQdToEvict_FinalizeBundles task : finalizeBundleThreads)
        {
            try {
                task.join(SHUTDOWN_LIMIT);
            } catch (InterruptedException e1) {
                log.debug("Bridge - finalizeBundleThreads interrupted", e1);
            }
        }

        if ((dataStore != null) && (dbConnectionManager != null))
        {
            try {
                dbConnectionManager.closeAllConnections();
            } catch (SQLException e) {
                log.debug("Bridge - dbConnectionManager interrupted", e);
            }
        }

        queueStatusThread.shutdown();

        aionJsonRpcConsolidator.getApi().evictConnections();
        ethJsonRpcConsolidator.getApi().evictConnections();

        executor.shutdown();
    }

    public static class Builder {

        // Required parameters
        SignatoryCollector signatoryCollector;
        AionClient aionClient;
        AionJsonRpcConsolidator aionJsonRpcConsolidator;
        EthJsonRpcConsolidator ethJsonRpcConsolidator;
        AionTipState aionTipState;
        Integer receiptMinDepth;
        Integer aionFinalizationLimit;
        AionUnbundlingPolicy unbundlingPolicy;
        EthBundlingPolicy ethBundlingPolicy;
        ChainLink startBlock;

        // Optional parameters
        DataStore dataStore;
        DbConnectionManager dbConnectionManager;
        ThreadPoolExecutor executor;

        String successfulTxHashEvent;

        public Builder setSignatoryCollector(SignatoryCollector x) {signatoryCollector = x; return this;}
        public Builder setDatabase(DataStore x, DbConnectionManager y) { dataStore = x; dbConnectionManager = y; return this; }
        public Builder setAionClient(AionClient x) {aionClient = x; return this;}
        public Builder setAionJsonRpcConsolidator(AionJsonRpcConsolidator x) {aionJsonRpcConsolidator = x; return this;}
        public Builder setEthJsonRpcConsolidator(EthJsonRpcConsolidator x) {ethJsonRpcConsolidator = x; return this;}
        public Builder setAionTipState(AionTipState x) {aionTipState = x; return this;}
        public Builder setReceiptMinDepth(Integer x) {receiptMinDepth = x; return this;}
        public Builder setAionFinalizationLimit(Integer x) {aionFinalizationLimit = x; return this;}
        public Builder setUnbundlingPolicy(AionUnbundlingPolicy x) {unbundlingPolicy = x; return this;}
        public Builder setEthBundlingPolicy(EthBundlingPolicy x) {ethBundlingPolicy = x; return this;}
        public Builder setStartBlock(ChainLink x) {startBlock = x; return this;}
        public Builder setSuccessfulTxHashEvent(String x) {successfulTxHashEvent = x; return this;}
        public Builder setExecutor(ThreadPoolExecutor x) {executor = x; return this;}


        public Bridge build() {
            if (allNotNull(signatoryCollector, aionClient, aionJsonRpcConsolidator, aionTipState, receiptMinDepth, aionFinalizationLimit, unbundlingPolicy)) {
                return new Bridge(this);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private class CriticalExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LoggingSetup.setupLogging();
            log.error(LoggingSetup.SMTP_MARKER, "Caught a critical error from thread: {}\n Message: {}\n Trace: {}\n", t.getName(), e.getMessage(), ExceptionUtils.getStackTrace(e));
            shutdown();
        }
    }

    private class QueueStatusThread {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        private final Logger log = LoggerFactory.getLogger(LogEnum.QUEUE.name());
        private final long INITIAL_DELAY = 15;
        private final long PERIOD = 30;


        public void start() {
            scheduledExecutorService.scheduleAtFixedRate(() ->
                            log.debug("Queue Sizes: Bundles: {}, Signed: {}, Submitted: {}, AwaitingFinalization: {}",
                                    QA_Bundles.size(), QB_SignedBundles.size(), QC_AwaitingReceiptBundles.size(), QD_AwaitingFinalizationBundles.size())
                    , INITIAL_DELAY, PERIOD, TimeUnit.SECONDS);
        }

        public void shutdown() {
            scheduledExecutorService.shutdown();
        }

    }
}
