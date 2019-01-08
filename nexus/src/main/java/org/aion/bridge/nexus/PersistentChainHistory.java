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

import org.aion.bridge.chain.base.oracle.ChainHistory;
import org.aion.bridge.chain.base.oracle.ChainOracleResultset;
import org.aion.bridge.chain.base.types.BlockWithReceipts;
import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.*;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;
import org.aion.bridge.datastore.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

@SuppressWarnings("Duplicates")
public class PersistentChainHistory implements ChainHistory<EthBlock, EthReceipt, EthLog> {

    private final Logger log = LoggerFactory.getLogger(PersistentChainHistory.class);

    private final DataStore ds;
    private final LinkedBlockingDeque<StatefulBundle> QA_Bundles;
    private final ChainLink startBlock;
    private EthBundlingPolicy ethBundlingPolicy;

    public PersistentChainHistory(DataStore ds,
                                  LinkedBlockingDeque<StatefulBundle> QA_Bundles,
                                  ChainLink startBlock,
                                  EthBundlingPolicy ethBundlingPolicy) {
        this.ds = ds;
        this.QA_Bundles = QA_Bundles;
        this.startBlock = startBlock;
        this.ethBundlingPolicy = ethBundlingPolicy;
    }

    @Override
    public ChainLink getLatestBlock() throws PersistenceServiceException {
        return ds.getEthFinalizedBlock().orElse(startBlock);
    }

    @Override
    public void reorganize(ChainLink historyHead, long chainHead, EthBlock chainAtHistoryHead) throws CriticalBridgeTaskException {
        // For now shutdown if we detect a reorg after our finalization limit

        StringBuilder sb = new StringBuilder();
        sb.append("Re-org detected in finalized blocks");
        sb.append("History block #: ");
        sb.append(historyHead.getNumber());
        sb.append("\n");
        sb.append("History block hash: ");
        sb.append(historyHead.getHash().toStringWithPrefix());
        sb.append("\n");
        sb.append("Found block #: ");
        sb.append(chainAtHistoryHead.getNumber());
        sb.append("\n");
        sb.append("Found block hash: ");
        sb.append(chainAtHistoryHead.getHash().toStringWithPrefix());
        sb.append("\n");

        log.error("Detected re-org in finalized blocks, History head: {}, {}, Chain head: {}, {}", historyHead.getNumber(), historyHead.getHash().toString(),
                chainAtHistoryHead.getNumber(), chainAtHistoryHead.getHash().toString());

        throw new CriticalBridgeTaskException(sb.toString());
    }

    // TODO: reloading the queue based on bundles from the DB
    @Override
    public void publish(ChainOracleResultset<EthBlock, EthReceipt, EthLog> rs) throws PersistenceServiceException, InterruptedException {

        if (rs.isOpen())
            throw new IllegalArgumentException("non-finalized result set should not be returned from oracle");

        long finalizedBundleId = ds.getEthFinalizedBundleId().orElse(-1L);
        Optional<ChainLink> finalizedBlock = ds.getEthFinalizedBlock();

        List<BlockWithReceipts<EthBlock, EthReceipt, EthLog>> blocks = rs.getAllBlocks();
        List<Bundle> bundles = new ArrayList<>();
        Map<Word32, EthAddress> txHashToEthAddressMap = new HashMap<>();

        // Check new batch of blocks connects to the top of the currently stored eth chain
        if (finalizedBlock.isPresent() && !blocks.isEmpty()) {
            if (!finalizedBlock.get().getHash().equals(blocks.get(0).getBlock().getParentHash())) {
                // Break in the chain, possible deep re-org.
                // Stop the bridge due to finalized blocks being re-org'd
                StringBuilder sb = new StringBuilder();
                sb.append("Break in source chain:");
                sb.append("Last finalized block #: ");
                sb.append(finalizedBlock.get().getNumber());
                sb.append("\n");
                sb.append("Last finalized block hash: ");
                sb.append(finalizedBlock.get().getHash().toStringWithPrefix());
                sb.append("\n");
                sb.append("Found block #: ");
                sb.append(blocks.get(0).getBlock().getNumber());
                sb.append("\n");
                sb.append("Found block hash: ");
                sb.append(blocks.get(0).getBlock().getHash().toStringWithPrefix());
                sb.append("\n");

                log.error("Break in source chain, Last finalized block: {}, {}, Next found block: {}, {}", finalizedBlock.get().getNumber(), finalizedBlock.get().getHash().toString(),
                        blocks.get(0).getBlock().getNumber(), blocks.get(0).getBlock().getHash().toString());

                throw new CriticalBridgeTaskException(sb.toString());
            }
        }

        for (BlockWithReceipts<EthBlock, EthReceipt, EthLog> block : blocks) {
            List<EthBundle> ethBundles = ethBundlingPolicy.fromFilteredBlock(block.getBlock(), block.getReceipts());

            for (EthBundle b : ethBundles) {
                bundles.add(b.getBundle());
                b.getTxHashToEthAddressMap().forEach((k, v) ->
                        txHashToEthAddressMap.merge(k, v, (v1, v2) -> {
                            // if the remapping function is called, that means the key already exists.
                            throw new AssertionError("Duplicate transaction hash reported: "+k.toString());
                        }));
            }
        }

        // default sort order is ascending. perf: nlgn
        bundles.sort(Bundle::compareTo);

        List<StatefulBundle> sbs = new ArrayList<>();
        for (Bundle bundle : bundles) {
            sbs.add(new StatefulBundle(new PersistentBundle(++finalizedBundleId, bundle)));
        }

        if (sbs.isEmpty())
            // No bundles to submit, update chain history
            ds.storeEthChainHistory(blocks.get(blocks.size() - 1).getBlock());
        else
            // flush to db first, then try to move the data into processing queue
            ds.storeEthFinalizedBundles(sbs,
                    blocks.get(blocks.size()-1).getBlock(),
                    sbs.get(sbs.size()-1),
                    txHashToEthAddressMap);

        // set the state to stored
        for (StatefulBundle sb : sbs) {
            sb.setStored();
            // spin here until we can enqueue, before moving forward and getting more blocks from network
            while (QA_Bundles.remainingCapacity() < 1 && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(5000L);
            }

            QA_Bundles.offer(sb);
        }
    }
}
