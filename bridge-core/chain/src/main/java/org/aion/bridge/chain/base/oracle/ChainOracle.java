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

package org.aion.bridge.chain.base.oracle;

import com.google.common.base.Stopwatch;
import org.aion.bridge.chain.base.BlockNumberCollector;
import org.aion.bridge.chain.base.BlockProcessor;
import org.aion.bridge.chain.base.BlockProcessorMissingReceiptsException;
import org.aion.bridge.chain.base.api.IncompleteApiCallException;
import org.aion.bridge.chain.base.api.MalformedApiResponseException;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.types.*;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.log.LogEnum;
import org.aion.bridge.chain.log.LoggingSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The tip distance refers to the distance from the tip that we sync to, the
 * rationale behind this is we want to avoid turbulent periods that can occur
 * when we sync close to the tip, (usually within the first 1-3 blocks).
 * <p>
 * Note: this does not mean that we do not support rebranching.
 */

// retrievedBlocks:
// represents a list of blocks that were successfully retrieved from the network
// the state PULL_RECEIPTS will attempt to pull receipts

// lastSuccessfullyRetrievedBlock:
// indicates the last retrieved block on a completion of a pull_receipt
// (the expectation is that this block is propagated onto the cluster)
// we use this to match against the first block of the next batch, if they
// do not match then this indicates to us that the blockchain may have re-organized

@ThreadSafe
public class ChainOracle<B extends Block, R extends Receipt<L>, L extends Log, A extends Address> extends Thread {

    // Builder-Constructed State
    // -------------------------
    private final ChainOracleEventBus<B, R, L> eventBus;
    private final StatelessChainConnection<B, R, L, A> chain;
    private final BlockNumberCollector<B, R, L, A> blockNumberCollector;
    private final EventFilter filter;
    private final ChainHistory<B, R, L> history;
    private final Logger log;

    private final int tipDistance;
    private final long haltDelay;
    private final TimeUnit haltDelayTimeUnit;

    private final int blockBatchSize;
    private final int receiptBatchSize;

    private static final int EXCEPTION_DELAY_MS = 100; // so we don't infinite-loop
    private static final int MAX_CONSECUTIVE_ERR = 10;
    private Stopwatch timer = Stopwatch.createUnstarted();

    public ChainOracle(ChainOracleBuilder<B, R, L, A> b) {
        chain = b.connection;
        filter = b.filter;

        tipDistance = b.tipDistance;
        blockBatchSize = b.blockBatchSize;
        receiptBatchSize = b.receiptBatchSize;

        haltDelay = b.haltDelay;
        haltDelayTimeUnit = b.haltDelayTimeUnit;

        history = b.history;
        eventBus = new ChainOracleEventBus<>();
        blockNumberCollector = b.blockNumberCollector;
        if (b.log == null) {
            log = LoggerFactory.getLogger(LogEnum.CHAIN_ORACLE.name());
        } else {
            log = b.log;
        }

        LoggingSetup.setupLogging();
    }

    private volatile boolean shutdown = false;

    public ChainOracleEventBus<B, R, L> getEventBus() {
        return eventBus;
    }

    // just delegate the reorganization responsibilities to the history object
    private void reorganize(ChainLink historyHead, long chainHead)
            throws MalformedApiResponseException, IncompleteApiCallException, InterruptedException, QuorumNotAvailableException {
        Optional<B> chainAtHistoryHead = chain.getBlock(historyHead.getNumber());
        history.reorganize(historyHead, chainHead, chainAtHistoryHead.orElse(null));
    }

    @Override
    public void run() {
        int errAccumulator = 0;
        while (!shutdown && errAccumulator < MAX_CONSECUTIVE_ERR) {
            try {
                ChainLink historyHead = history.getLatestBlock();
                Optional<Long> latestBlockNumber = blockNumberCollector.getLatestBlockNumber();

                if (!latestBlockNumber.isPresent()) {
                    throw new ChainOracleValidationException("ChainOracle: Latest block number not found. Restarting loop");
                }

                long chainHead = Long.max(latestBlockNumber.get() - tipDistance, 0L);

                // latestBroadcastBlock will be made consistent if re-org happens
                if (chainHead == historyHead.getNumber()) {
                    log.debug("ChainOracle: Caught up to Eth head at #{}", chainHead);
                    haltDelayTimeUnit.sleep(haltDelay);
                    continue;
                } else if (chainHead < historyHead.getNumber()) {
                    // case where the latest chain block < the block we've previously broadcast
                    log.error("ChainOracle: ");
                    reorganize(historyHead, chainHead);
                    TimeUnit.MILLISECONDS.sleep(EXCEPTION_DELAY_MS);
                    continue;
                }

                // note: closed range: [inclusive, inclusive]
                long start = historyHead.getNumber() + 1;
                long end = Long.min(start + blockBatchSize - 1, chainHead);

                log.debug("Querying for block range [{} - {}]", start, end);
                timer.reset().start();
                // get a 'well-ordered' list of blocks from api (see enforced contract)
                List<B> pulledBlocks = chain.getBlocksRangeClosed(start, end);
                ;
                log.info("Block range [{} - {}] retrieved in {}",
                        pulledBlocks.get(0).getNumber(), pulledBlocks.get(pulledBlocks.size() - 1).getNumber(), timer.stop().toString());

                if (!pulledBlocks.get(0).getParentHash().equals(historyHead.getHash())) {
                    // case where the latest chain block's ancestor != block we've previously broadcast
                    reorganize(historyHead, chainHead);
                    TimeUnit.MILLISECONDS.sleep(EXCEPTION_DELAY_MS);
                    continue;
                }

                // allocate the object that holds the result set
                ChainOracleResultset<B, R, L> rs = new ChainOracleResultset<>();

                // filter out 'emptyBlocks' based on the block-level bloom
                List<B> potentialFilledBlocks = filterAtBlockLevel(pulledBlocks, rs);

                log.debug("Querying for receipt count=[{}]", potentialFilledBlocks.size());
                timer.reset().start();
                // any blocks we *suspect* have bridge transactions, retrieve from the api
                List<BlockWithReceipts<B, R, L>> pulledReceipts = getReceiptsForBlocks(potentialFilledBlocks);
                log.info("Receipt count [{}] retrieved in {}", potentialFilledBlocks.size(), timer.stop().toString());

                // filter out only bridge transactions and populate resultset with 'bridge blocks'
                filterAtReceiptLevel(pulledReceipts, rs);

                // if we got here, no-one threw an exception, so everything went well.
                rs.finalizeResultset();

                // make sure we didn't miss any blocks
                if (!isBlockListIntegrityOK(rs.getAllBlocks())) {
                    throw new ChainOracleValidationException("ChainOracle - block list integrity check failed. Restarting the loop.");
                }

                // publish the result-set to be added to history
                history.publish(rs);

                // reset error accumulator since all went well
                errAccumulator = 0;

                // in the success case, there is nothing checking interrupt flag has been triggered; check it here.
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Interrupted flag triggered on success path through ChainOracle");
                }
            }
            catch (MalformedApiResponseException | IncompleteApiCallException | QuorumNotAvailableException |
                    BlockProcessorMissingReceiptsException | ChainOracleValidationException e) {
                // checked exceptions we might be able to recover from if we re-try
                try {
                    if (timer.isRunning()) {
                        timer.stop().reset();
                    }
                    TimeUnit.MILLISECONDS.sleep(EXCEPTION_DELAY_MS);
                } catch (InterruptedException f) {
                    log.info("Interrupted via InterruptedException");
                    throw new RuntimeException(e.getCause());
                }

                errAccumulator++;
                log.error("Caught recoverable exception. Retrying ...", e);
            } catch (InterruptedException e) {
                log.info("Interrupted via InterruptedException");
                throw new RuntimeException(e.getCause());
            } catch (PersistenceServiceException e) {
                // crash the process, since we can't recover from a persistence exception
                log.error("Caught a PersistenceServiceException. Crashing the process.");
                throw new RuntimeException(e.getCause());
            }
            // all other exceptions, let them just crash the process
        }

        if (errAccumulator >= MAX_CONSECUTIVE_ERR) {
            log.debug("ChainOracle - exited due to [errAccumulator >= MAX_CONSECUTIVE_ERR] ({})", errAccumulator);
            throw new RuntimeException("ChainOracle - exited due to [errAccumulator >= MAX_CONSECUTIVE_ERR]");
        }

        log.debug("Exiting gracefully ...");
    }

    // any blocks we *suspect* have bridge transactions, retrieve from the api
    private List<BlockWithReceipts<B, R, L>> getReceiptsForBlocks(List<B> blocks)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException {

        if (blocks.size() == 0)
            return new ArrayList<>();

        List<B> requestedBlocks = Collections.unmodifiableList(blocks);

        Set<Word32> paranoidBlockHashList = new HashSet<>();
        for (B b : requestedBlocks) {
            paranoidBlockHashList.add(b.getHash());
        }

        List<BlockWithReceipts<B, R, L>> response = new ArrayList<>();

        // generate sub-lists for batches if the batch size is too large
        int blocksPtr = 0;
        while (blocksPtr < requestedBlocks.size()) {
            List<B> batch = new ArrayList<>();
            int requestedTxCount = 0;
            do {
                B b = requestedBlocks.get(blocksPtr);
                requestedTxCount += b.getTransactionHashes().size();
                batch.add(b);
                blocksPtr++;
            } while (blocksPtr < requestedBlocks.size() &&
                    (requestedTxCount + requestedBlocks.get(blocksPtr).getTransactionHashes().size()) <= receiptBatchSize);

            if (batch.size() < 1)
                throw new IllegalStateException("ChainOracle - IllegalState: batch cannot be of size 0");

            log.trace("ChainOracle - getReceiptsForBlocks: Requesting [{}] receipts for blocks [{} of {}]", requestedTxCount, batch.size(), requestedBlocks.size());

            List<BlockWithReceipts<B, R, L>> pulled = chain.getReceiptsForBlocks(batch);

            for (BlockWithReceipts<B, R, L> bwr : pulled) {
                response.add(bwr);
                paranoidBlockHashList.remove(bwr.getBlock().getHash());
            }
        }

        if (!paranoidBlockHashList.isEmpty()) {
            throw new MalformedApiResponseException("ChainOracle getReceiptsForBlocks - Paranoid block hash list not empty at end of processing");
        }

        return response;
    }

    private List<B> filterAtBlockLevel(final List<B> pulledBlocks,
                                       final ChainOracleResultset<B, R, L> resultset) {
        List<B> potentialFilledBlocks = new ArrayList<>(pulledBlocks.size());
        for (B b : pulledBlocks) {
            if (!BlockProcessor.filterBlock(b, filter)) {
                resultset.appendEmptyBlock(b);
            } else {
                potentialFilledBlocks.add(b);
            }
        }
        return potentialFilledBlocks;
    }

    /**
     * This method has 3 major responsibilities;
     * <p>
     * Note: Responsibilities were combined to not have to iterate over the collection multiple times.
     * <p>
     * 1. Validate that a receipt was returned for every transaction hash requested
     * => if this is false, one of two things probably happened:
     * a) kernel-side API errored out
     * b) chain re-organized from under us
     * 2. Iterate over all receipts to filter OUT transactions NOT matching our contract filter
     * 3. Take these filtered receipt set and populate result-set with either empty or bridge blocks
     */
    private void filterAtReceiptLevel(final List<BlockWithReceipts<B, R, L>> pulledReceipts,
                                      final ChainOracleResultset<B, R, L> resultset) throws BlockProcessorMissingReceiptsException {

        for (BlockWithReceipts<B, R, L> entry : pulledReceipts) {
            B block = entry.getBlock();
            List<R> receipts = entry.getReceipts();

            // delegate the filtering to BlockProcessor
            List<R> filteredReceipts = BlockProcessor.filterReceipts(block, receipts, filter);

            if (filteredReceipts.isEmpty()) {
                resultset.appendEmptyBlock(block);
            } else {
                resultset.appendFilledBlock(new BlockWithReceipts<>(block, filteredReceipts));
            }
        }
    }

    public void shutdown() {
        this.shutdown = true;
        log.debug("ChainOracle - shutdown signal received");
    }

    /**
     * returns {@code true} if integrity constraints check out:
     * <p>
     * 1. block numbers SHALL be sequential, in ascending order (ie. lowest block number at index 0)
     * 2. parent hashes SHALL be consistent; ie. block[n].parentHash == block[n-1].hash
     * 3. input list does not contain any nulls
     */
    private boolean isBlockListIntegrityOK(List<BlockWithReceipts<B, R, L>> blocks) {
        BlockWithReceipts<B, R, L> previous = null;
        for (BlockWithReceipts<B, R, L> b : blocks) {

            if (b == null) return false;

            if (previous == null) {
                previous = b;
                continue;
            }

            if (!previous.getBlock().getHash().equals(b.getBlock().getParentHash())) return false;
            if (previous.getBlock().getNumber() + 1 != b.getBlock().getNumber()) return false;

            previous = b;
        }

        return true;
    }

    private static class ChainOracleValidationException extends Exception {
        ChainOracleValidationException(String msg) { super(msg); }
    }
}



































































