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

package org.aion.monitor;

import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.log.LoggingSetup;
import org.aion.monitor.api.EthApi;
import org.aion.monitor.datastore.DatabaseOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BlockRangeValidator extends Thread {

    private static final Logger log = LoggerFactory.getLogger(BlockRangeValidator.class);

    private Map<Integer, NodeInformation> nodeIdBlockNumberMap = new HashMap<>();
    private List<EthApi> ethNodeConnection;
    private EthApi infuraApi;
    private EthApi etherscanApi;
    private DatabaseOperations dbOperations;
    private long pollIntervalSeconds;
    private int maxAcceptedRange;
    private int acceptedUnresponsiveCount;
    private int acceptedSideChainLength;
    private List<Boolean> notificationSent = new ArrayList<>();
    private List<Integer> sideChainBlocks = new ArrayList<>();

    BlockRangeValidator(@Nonnull List<EthApi> ethNodeConnection,
                        @Nonnull EthApi infuraApi,
                        @Nonnull EthApi etherscanApi,
                        @Nonnull int maxAcceptedRange,
                        @Nonnull int acceptedUnresponsiveCount,
                        @Nonnull int acceptedSideChainLength,
                        @Nonnull long pollIntervalSeconds,
                        @Nonnull DatabaseOperations dbOperations) {
        this.ethNodeConnection = ethNodeConnection;
        for (EthApi c : ethNodeConnection) {
            nodeIdBlockNumberMap.put(c.getId(), new NodeInformation(maxAcceptedRange));
            notificationSent.add(false);
            sideChainBlocks.add(0);
        }
        this.maxAcceptedRange = maxAcceptedRange;
        this.acceptedUnresponsiveCount = acceptedUnresponsiveCount;
        this.acceptedSideChainLength = acceptedSideChainLength;
        this.infuraApi = infuraApi;
        this.etherscanApi = etherscanApi;
        this.dbOperations = dbOperations;
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Object[][] result = new Object[ethNodeConnection.size()][];

                List<Optional<EthBlock>> blockList = new ArrayList<>();
                List<Integer> peerCountList = new ArrayList<>();

                //get latest block and peer count for node
                for (EthApi ethJsonRpcApi : ethNodeConnection) {
                    Optional<EthBlock> nodeLatestBlockList = ethJsonRpcApi.getBLock(ethJsonRpcApi.getLatestBLockNumber());
                    blockList.add(nodeLatestBlockList);
                    peerCountList.add(ethJsonRpcApi.getPeerCount());
                }

                //get latest block number for apis
                Long infuraLatestBlockNumber = infuraApi.getLatestBLockNumber();
                Long etherscanLatestBlockNumber = etherscanApi.getLatestBLockNumber();

                if (infuraLatestBlockNumber == null || etherscanLatestBlockNumber == null) {
                    continue;
                }

                for (int i = 0; i < blockList.size(); i++) {
                    Optional<EthBlock> b = blockList.get(i);
                    String name = ethNodeConnection.get(i).getName();
                    NodeInformation nodeInfo = nodeIdBlockNumberMap.get(ethNodeConnection.get(i).getId());

                    if (b.isPresent()) {
                        nodeInfo.resetBlockNotPresentCount();
                        Long heightChange = nodeInfo.getLatestBlockNumber() == 0 ? 0 : b.get().getNumber() - nodeInfo.getLatestBlockNumber();
                        nodeInfo.setLatestBlockNumber(b.get().getNumber());

                        Long infuraDiff = infuraLatestBlockNumber - b.get().getNumber();
                        Long etherscanDiff = etherscanLatestBlockNumber - b.get().getNumber();
                        nodeInfo.updateApiDifference(infuraDiff, etherscanDiff);

                        result[i] = new String[]{name, Long.toString(b.get().getNumber()), Long.toString(heightChange), Long.toString(infuraDiff),
                                Long.toString(nodeInfo.getMaxInfuraDiff()), nodeInfo.getCountOutOfRangeInfura().toString(),
                                Long.toString(etherscanDiff), Long.toString(nodeInfo.getMaxEtherscanDiff()), nodeInfo.getCountOutOfRangeEtherscan().toString(), String.valueOf(peerCountList.get(i))};

                        try {
                            dbOperations.insertVarianceData(ethNodeConnection.get(i).getId(), b.get().getNumber(), heightChange, infuraDiff, etherscanDiff, peerCountList.get(i));
                        } catch (PersistenceServiceException e) {
                            //todo
                            e.printStackTrace();
                        }

                        if (infuraDiff > maxAcceptedRange || etherscanDiff > maxAcceptedRange) {
                            if (!notificationSent.get(i)) {
                                log.error("{}: {} block number is lower than accepted range. blk_num: {}, current_block: {}", name, ethNodeConnection.get(i).getUrl(), b.get().getNumber(), infuraLatestBlockNumber);
                                sendEmail();
                                notificationSent.set(i, true);
                            }
                        } else {
                            notificationSent.set(i, false);
                        }

                        validateBlockHash(i, b.get());

                        // node did not return a block
                    } else {
                        result[i] = new String[]{name, "Error", "Node's latest block not present.", "", "", "", "", "", "", String.valueOf(peerCountList.get(i))};
                        nodeInfo.incrementBlockNotPresentCount();
                        if (nodeInfo.getBlockNotPresentCount() == acceptedUnresponsiveCount) {
                            log.error("{}: {} is not responding to getBlock requests.", name, ethNodeConnection.get(i).getUrl());
                            sendEmail();
                        }
                    }
                }

                System.out.format("%-20s%-10s%-15s%-14s%-10s%-15s%-14s%-10s%-15s%-10s\n",
                        "name", "blk-Num", "height-Change", "<Infura-diff", "max_diff", "out_of_range>", "<Ether-diff", "max_diff", "out_of_range>", "peers");
                for (final Object[] row : result) {
                    System.out.format("%-20s%-10s%-15s%-14s%-10s%-15s%-14s%-10s%-15s%-10s\n", row);
                }

                System.out.println("\n");

                try {
                    TimeUnit.SECONDS.sleep(pollIntervalSeconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.out.println("caught an exception.");
                e.printStackTrace();
            }
        }
    }


    private void validateBlockHash(int index, EthBlock b) {

        Optional<EthBlock> infuraBlock = infuraApi.getBLock(b.getNumber());
        Optional<EthBlock> etherBlock = etherscanApi.getBLock(b.getNumber());
        int count = 0;

        if ((infuraBlock.isPresent() && !infuraBlock.get().equals(b)) || (etherBlock.isPresent() && !etherBlock.get().equals(b))) {
            count = sideChainBlocks.get(index) + 1;
            if (count > acceptedSideChainLength) {
                log.error("{}: {} block hashes do not match Infura/Etherscan.", ethNodeConnection.get(index).getName(), ethNodeConnection.get(index).getUrl());
                sendEmail();
            }
        }
        sideChainBlocks.set(index, count);
    }

    private void sendEmail() {
        LoggingSetup.setupLogging();
        log.error(LoggingSetup.SMTP_MARKER, "Critical error in an eth node.");
    }

    public static class NodeInformation {
        private Integer countOutOfRangeInfura = 0;
        private long latestBlockNumber = 0;
        private long maxInfuraDiff = 0;
        private int maxAcceptedBlockRange;
        private Integer countOutOfRangeEtherscan = 0;
        private long maxEtherscanDiff = 0;
        private int blockNotPresentCount = 0;

        NodeInformation(int maxAcceptedBlockRange) {
            this.maxAcceptedBlockRange = maxAcceptedBlockRange;
        }

        Integer getCountOutOfRangeInfura() {
            return countOutOfRangeInfura;
        }

        long getLatestBlockNumber() {
            return latestBlockNumber;
        }

        void setLatestBlockNumber(long latestBlockNumber) {
            this.latestBlockNumber = latestBlockNumber;
        }

        long getMaxInfuraDiff() {
            return maxInfuraDiff;
        }

        Integer getCountOutOfRangeEtherscan() {
            return countOutOfRangeEtherscan;
        }

        long getMaxEtherscanDiff() {
            return maxEtherscanDiff;
        }

        void updateApiDifference(long infuraDiff, long EtherscanDiff) {
            if (infuraDiff > this.maxInfuraDiff) {
                this.maxInfuraDiff = infuraDiff;
            }
            if (infuraDiff > maxAcceptedBlockRange)
                countOutOfRangeInfura++;

            if (EtherscanDiff > this.maxEtherscanDiff) {
                this.maxEtherscanDiff = infuraDiff;
            }
            if (EtherscanDiff > maxAcceptedBlockRange)
                countOutOfRangeEtherscan++;
        }

        void resetBlockNotPresentCount() {
            blockNotPresentCount = 0;
        }

        int getBlockNotPresentCount() {
            return blockNotPresentCount;
        }

        void incrementBlockNotPresentCount() {
            blockNotPresentCount++;
        }
    }
}



