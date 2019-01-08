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

package org.aion.bridge.standby;

import org.aion.bridge.chain.aion.types.AionBlock;
import org.aion.bridge.chain.aion.types.AionLog;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.oracle.ChainHistory;
import org.aion.bridge.chain.base.oracle.ChainOracleResultset;
import org.aion.bridge.chain.base.types.BlockWithReceipts;
import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.*;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.datastore.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@SuppressWarnings("Duplicates")
public class AionChainHistory implements ChainHistory<AionBlock, AionReceipt, AionLog> {

    private final Logger LOG = LoggerFactory.getLogger(AionChainHistory.class);

    private final DataStore ds;
    private final ChainLink historyStart;
    private static final long DELAY_SECONDS = 10;
    private final AionUnbundlingPolicy unbundlingPolicy;

    public AionChainHistory(DataStore ds,
                            ChainLink historyStart,
                            AionUnbundlingPolicy unbundlingPolicy) {
        this.ds = ds;
        this.historyStart = historyStart;
        this.unbundlingPolicy = unbundlingPolicy;
    }

    @Override
    public ChainLink getLatestBlock() throws PersistenceServiceException {
        return ds.getAionFinalizedBlock().orElse(historyStart);
    }

    @Override
    public void reorganize(ChainLink historyHead, long chainHead, AionBlock chainAtHistoryHead) {
        throw new RuntimeException("Re-org should not happen");
    }

    @Override
    public void publish(ChainOracleResultset<AionBlock, AionReceipt, AionLog> rs) throws PersistenceServiceException, InterruptedException {

        if (rs.isOpen())
            throw new IllegalArgumentException("Non-finalized result set should not be returned from oracle");

        // Retrieve all the blocks from the result set; they are guaranteed to be in ascending order.
        List<BlockWithReceipts<AionBlock, AionReceipt, AionLog>> blocks = rs.getAllBlocks();
        AionBlock blockEarliest = blocks.get(0).getBlock();
        AionBlock blockLatest = blocks.get(blocks.size() - 1).getBlock();

        // Check if the new batch of blocks connects to the too of database chain. If not, fail out of here.
        Optional<ChainLink> finalizedBlock = ds.getAionFinalizedBlock();
        if(finalizedBlock.isPresent() && !blocks.isEmpty()) {
            if(!finalizedBlock.get().getHash().equals(blockEarliest.getParentHash())) {
                throw new RuntimeException("Break in source chain");
            }
        }

        // Anything actually finalized in this bundle?
        List<AionBundle> finalizedBundles = new ArrayList<>();

        for (BlockWithReceipts<AionBlock, AionReceipt, AionLog> block : blocks) {
            finalizedBundles.addAll(unbundlingPolicy.fromFilteredReceipt(block.getReceipts()));
        }

        // Nothing confirmed in this batch of receipts, update DB and return control to ChainOracle
        if (finalizedBundles.size() == 0) {
            ds.storeAionChainHistory(new ChainLink(blockLatest.getNumber(), blockLatest.getHash()));
            return;
        }

        // Ok, so some bundles that may be finalized in this batch
        // -------------------------------------------------------

        // Get last finalized bundle id's
        Optional<Long> aionFinalizedBundleId = ds.getAionFinalizedBundleId();
        Optional<Long> ethFinalizedBundleId;
        Optional<List<PersistentBundle>> nonFinalizedBundles;

        while (true) {
            ethFinalizedBundleId = ds.getEthFinalizedBundleId();

            // Eth processor hasn't kicked in yet
            if (!ethFinalizedBundleId.isPresent()) {
                LOG.info("Ethereum History is empty. Sleeping {}s.", DELAY_SECONDS);
                TimeUnit.SECONDS.sleep(DELAY_SECONDS);
                continue;
            }

            // Maybe the Eth side has not caught up yet?
            if (aionFinalizedBundleId.isPresent()) {
                int compare = ethFinalizedBundleId.get().compareTo(aionFinalizedBundleId.get());

                if (compare < 0) {
                    throw new IllegalStateException("Aion Finalized Bundle Id [" + aionFinalizedBundleId.get() + "] found " +
                            "to be greater than Eth Finalized Bundle Id [" + ethFinalizedBundleId.get() + "]");
                }
                else if (compare == 0) {
                    LOG.info("Aion all caught up to Ethereum History. EthFinalized[{}] == AionFinalized[{}]. Sleeping {}s.",
                            ethFinalizedBundleId.get(), aionFinalizedBundleId.get(), DELAY_SECONDS);
                    TimeUnit.SECONDS.sleep(DELAY_SECONDS);
                    continue;
                }
            }

            // Found something finalized in this bundle, check if its part of outstanding bundle list
            Long start;
            Long end;

            // Nothing stored in the finalized db on Aion yet. Grab everything in the table that has been finalized on Eth
            if (!aionFinalizedBundleId.isPresent()) {
                start = 0L;
                end = ethFinalizedBundleId.get();
            }
            // Get the range of bundles from the DB between finalization indexes on Eth and Aion
            else {
                start = aionFinalizedBundleId.get() + 1;
                end = ethFinalizedBundleId.get();
            }

            if (end < start)
                throw new IllegalStateException("Inconsistent DB state; Eth Finalized Bundle Id=" + end + " cannot be less than Eth Finalized Bundle Id=" + start);

            LOG.debug("Non-finalized bundles found on Ethereum: [{} - {}]", start, end);

            /**
             * Since a single Aion block can have multiple batches in it, we need to make sure the Eth history has sufficiently
             * caught up to have populated the Bundles DB with all the bundles we've seen in this Aion block.
             *
             * So if not enough bundles have been populated in the DB from the Eth side, wait for a little bit and try again.
             */
            long nonFinalizedBundlesSize = end - start + 1;
            if (finalizedBundles.size() > nonFinalizedBundlesSize) {
                LOG.info("Finalized Bundle Count=[{}] > Non-Finalized Bundle Count=[{}]. " +
                                "Ethereum History not caught up to Aion History. Sleeping {}s.",
                        finalizedBundles.size(), nonFinalizedBundlesSize, DELAY_SECONDS);
                TimeUnit.SECONDS.sleep(DELAY_SECONDS);
                continue;
            }

            nonFinalizedBundles = ds.getBundleRangeClosed(start, end);

            if (!nonFinalizedBundles.isPresent() || nonFinalizedBundles.get().size() != nonFinalizedBundlesSize) {
                throw new IllegalStateException("Inconsistent DB state; Eth Finalized Bundle table does not " +
                        "contain bundleId range: [" + start + " - " + end + "]");
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Interruption detected by AionChainHistory");
            }

            break;
        }

        Map<Word32, AionBundle> finalizedBundleMap = finalizedBundles.stream()
                .collect(toMap(AionBundle::getBundleHash, Function.identity()));

        Map<Word32, PersistentBundle> dbBundleMap = nonFinalizedBundles.get().stream()
                .collect(toMap(pb -> pb.getBundle().getBundleHash(), Function.identity()));

        // All finalized bundles reported by Aion must be present in the Eth Finalized database
        if (!dbBundleMap.keySet().containsAll(finalizedBundleMap.keySet())) {
            throw new IllegalStateException("Inconsistent DB state; Bundle hashes reported by Aion not found in DB.");
        }

        List<StatefulBundle> sbs = new ArrayList<>();

        for (AionBundle finalizedBundle : finalizedBundleMap.values()) {
            PersistentBundle dbBundle = dbBundleMap.get(finalizedBundle.getBundleHash());
            List<Transfer> dbTransfers = dbBundle.getBundle().getTransfers();
            List<Transfer> finalizedTransfers = finalizedBundle.getTransfers();

            // All transfers reported by Aion must be present in the database
            if (!(dbTransfers.size() == finalizedTransfers.size() && dbTransfers.containsAll(finalizedTransfers))) {
                throw new IllegalStateException("Inconsistent DB state; Transfers hashes reported on Aion not found in DB.");
            }

            StatefulBundle sb = new StatefulBundle(dbBundle);
            sb.setStored();
            sb.setSigned(null);
            sb.setSubmitted(null);
            sb.setSealed(finalizedBundle.getReceipt());
            sb.setFinalized();

            sbs.add(sb);
        }

        // Sort the bundles and make sure contiguous
        sbs.sort(StatefulBundle::compareTo);
        if (!isBundleIdContiguous(sbs)) {
            throw new IllegalStateException("Inconsistent Aion state; Found a hole in Bundles reported on Aion (according to order in DB).");
        }

        LOG.debug("Marking BundleId range: [{} - {}] as Aion Finalized", sbs.get(0).getBundleId(), sbs.get(sbs.size() - 1).getBundleId());

        ds.storeAionFinalizedBundles(sbs,
                                    new ChainLink(blockLatest.getNumber(), blockLatest.getHash()),
                                    sbs.get(sbs.size() - 1));
    }

    private boolean isBundleIdContiguous(List<StatefulBundle> bundles) {
        Long prev = null;
        for (StatefulBundle b : bundles) {
            if (prev == null) {
                prev = b.getBundleId();
                continue;
            }

            long current = b.getBundleId();
            if (current != prev + 1)
                return false;

            prev = current;
        }

        return true;
    }
}
