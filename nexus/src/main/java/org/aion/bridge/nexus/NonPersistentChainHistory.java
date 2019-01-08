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
import org.aion.bridge.chain.bridge.*;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import static java.util.stream.Collectors.toList;

public class NonPersistentChainHistory implements ChainHistory<EthBlock, EthReceipt, EthLog> {

    private LinkedBlockingDeque<StatefulBundle> QA_Bundles;
    private ChainLink chainLink;
    private final Logger log = LoggerFactory.getLogger(NonPersistentChainHistory.class);
    private int id = 0;
    private EthBundlingPolicy ethBundlingPolicy;

    public NonPersistentChainHistory(LinkedBlockingDeque<StatefulBundle> QA_Bundles, ChainLink startBlock, EthBundlingPolicy ethBundlingPolicy) {
        this.QA_Bundles = QA_Bundles;
        chainLink = startBlock;
        this.ethBundlingPolicy = ethBundlingPolicy;
    }

    @Override
    public ChainLink getLatestBlock() {
        return chainLink;
    }

    @Override
    public void reorganize(ChainLink historyHead, long chainHead, EthBlock chainAtHistoryHead) throws CriticalBridgeTaskException {
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

    @Override
    public void publish(ChainOracleResultset<EthBlock, EthReceipt, EthLog> rs) throws CriticalBridgeTaskException, InterruptedException {
        // Sort and finalize to stop any further changes to the result set
        rs.finalizeResultset();

        List<BlockWithReceipts<EthBlock, EthReceipt, EthLog>> blocks = rs.getAllBlocks();
        List<StatefulBundle> bundles = new ArrayList<>();

        // Assume blocks have been sorted, traverse blocks to extract all transfers
        for (BlockWithReceipts<EthBlock, EthReceipt, EthLog> block : blocks) {
            List<EthBundle> ethBundles = ethBundlingPolicy.fromFilteredBlock(block.getBlock(), block.getReceipts());
            List<Bundle> newBundles = ethBundles.stream().map(EthBundle::getBundle).collect(toList());
            for (Bundle b : newBundles) {
                bundles.add(new StatefulBundle(new PersistentBundle(id, b)));
                id++;
            }
        }

        ChainLink lastBlockInResponse = blocks.get(blocks.size()-1).getBlock();

        chainLink = lastBlockInResponse;
        for(StatefulBundle b: bundles) {
            log.info("Bundle {} enqueued", b.getBundleId());
            for(Transfer t : b.getTransfers()) {
                log.info("TxHash: {}, To: {}, Amount: {}", t.getEthTxHash(), t.getAionAddress(), t.getAionTransferAmount()); //Very verbose, probably remove for production
            }

            b.setStored();

            // spin here until we can enqueue, before moving forward and getting more blocks from network
            while(QA_Bundles.remainingCapacity() < 1 && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(5000L);
            }

            QA_Bundles.offer(b);
        }
    }
}
