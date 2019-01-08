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

package org.aion.bridge.transferMetrics.transfer;

import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.aion.types.AionBlock;
import org.aion.bridge.chain.aion.types.AionLog;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.api.IncompleteApiCallException;
import org.aion.bridge.chain.base.api.MalformedApiResponseException;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.bridge.AionFinalizedBundle;
import org.aion.bridge.chain.bridge.PersistentBundle;
import org.aion.bridge.chain.bridge.Transfer;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;
import org.aion.bridge.datastore.DataStore;
import org.aion.bridge.transferMetrics.TimedTransfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TransferLatencyCalculator {

    private DataStore dataStore;
    private StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> ethConnection;
    private StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> aionConnection;
    private final int AION_FINALIZATION = 90;
    private static final int maxmumRetry = 10;

    public void setNewStartBundleId(Long newStartBundleId) {
        this.newStartBundleId = newStartBundleId;
    }

    private Long newStartBundleId;

    public Long getNewStartBundleId() {
        return newStartBundleId;
    }



    public TransferLatencyCalculator(DataStore dataStore,
                                     StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> ethConnection,
                                     StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> aionConnection) {
        this.dataStore = dataStore;
        this.ethConnection = ethConnection;
        this.aionConnection = aionConnection;
    }

    public List<TimedTransfer> getTransferLatency(Long startBundle, Long aionFinalizedBundleId) throws MalformedApiResponseException, IncompleteApiCallException, InterruptedException, QuorumNotAvailableException, PersistenceServiceException {
        List<TimedTransfer> timedTransfers = new ArrayList<>();

        Optional<List<PersistentBundle>> bundleListOpt = dataStore.getBundleRangeClosed(startBundle, aionFinalizedBundleId);
        Optional<List<AionFinalizedBundle>> aionFinalizedOpt = dataStore.getAionTxHashRangeClosed(startBundle, aionFinalizedBundleId);

        if (!bundleListOpt.isPresent() || !aionFinalizedOpt.isPresent()) {
            System.out.println("Inconsistent DB state");
            System.exit(0);
        }

        List<PersistentBundle> ethFinalizedBundles = bundleListOpt.get();
        List<AionFinalizedBundle> aionFinalizedBundles = aionFinalizedOpt.get();

        Collections.sort(ethFinalizedBundles);
        Collections.sort(aionFinalizedBundles);


        // Grabbed bundles from the DB
        for (int i = 0; i < ethFinalizedBundles.size(); i++) {
            // Grab first transfer in the bundle and get timestamp
            // Timestamp will be the same for all transfers since they are all in a single block

            if (ethFinalizedBundles.get(i).getBundleId() != aionFinalizedBundles.get(i).getBundleId()) {
                System.out.println("Inconsistent DB state");
                System.exit(-1);
            }

            int numRequest = 0;

            while(numRequest < maxmumRetry) {

                Optional<EthBlock> ethRequest = ethConnection.getBlock(ethFinalizedBundles.get(i).getBundle().getEthBlockNumber());

                // This block must exist as the bundle is present in the finalization table.
                Optional<AionBlock> aionRequest = aionConnection.getBlock(aionFinalizedBundles.get(i).getAionBlockNumber() + AION_FINALIZATION);

                if (ethRequest.isPresent() && aionRequest.isPresent()) {
                    for (Transfer t : ethFinalizedBundles.get(i).getBundle().getTransfers()) {
                        timedTransfers.add(new TimedTransfer(t, ethRequest.get().getTimestamp(), aionRequest.get().getTimestamp()));
                    }

                    break;
                } else {
                    if(!aionRequest.isPresent()) {
                        System.out.println("Unable to retrieve aion block: " + aionFinalizedBundles.get(i).getAionBlockNumber() + AION_FINALIZATION );
                    } else {
                        System.out.println("Unable to retrieve eth block: " + ethFinalizedBundles.get(i).getBundle().getEthBlockNumber() );
                    }
                    TimeUnit.SECONDS.sleep(30);
                    numRequest++;
                }
            }

            if(numRequest >= maxmumRetry) {
                System.out.println("Unable to retrieve transfer data; max retry reached");
                System.exit(-2);
            }

        }

        newStartBundleId = ethFinalizedBundles.get(ethFinalizedBundles.size() - 1).getBundleId() + 1;

        return timedTransfers;
    }

}