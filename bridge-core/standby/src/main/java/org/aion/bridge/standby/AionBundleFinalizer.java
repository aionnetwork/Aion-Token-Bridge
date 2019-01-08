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

import com.google.common.base.Stopwatch;
import org.aion.bridge.chain.aion.rpc.FvmAbiCodec;
import org.aion.bridge.chain.aion.rpc.abi.FvmBytes32;
import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.aion.types.AionBlock;
import org.aion.bridge.chain.aion.types.AionLog;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.api.IncompleteApiCallException;
import org.aion.bridge.chain.base.api.MalformedApiResponseException;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.PersistentBundle;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.datastore.AionTipState;
import org.aion.bridge.datastore.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.ObjectUtils.allNotNull;

@SuppressWarnings("Duplicates")
public class AionBundleFinalizer extends Thread {

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        // Required parameters
        private DataStore ds;
        private StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> api;
        private AionTipState tipState;
        private AionAddress contractAddress;

        // Optional parameters
        private long haltDelaySeconds = 10;
        private long finalityDepth = 16;

        public Builder setDatabase(DataStore x) { ds = x; return this; }
        public Builder setApi(StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> x) { api = x; return this; }
        public Builder setTipState(AionTipState x) { tipState = x; return this; }
        public Builder setContractAddress(AionAddress x) { contractAddress = x; return this; }
        public Builder setHaltDelaySeconds(long x) { haltDelaySeconds = x; return this; }
        public Builder setFinalityDepth(long x) { finalityDepth = x; return this; }

        public AionBundleFinalizer build() {
            if (!allNotNull(ds, api, tipState, contractAddress))
                throw new IllegalStateException("!allNotNull(ds, api, tipState, contractAddress)");

            if (haltDelaySeconds < 1 || finalityDepth < 0)
                throw new IllegalStateException("haltDelaySeconds < 1 || finalityDepth < 0");

            return new AionBundleFinalizer(this);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(AionBundleFinalizer.class);

    private volatile boolean shutdown = false;

    private final DataStore ds;
    private final StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> api;
    private final AionTipState tipState;
    private final AionAddress contractAddress;

    private final long haltDelaySeconds;
    private final long finalityDepth;

    private static final int EXCEPTION_DELAY_MS = 100; // so we don't infinite-loop
    private static final int MAX_CONSECUTIVE_ERR = 10;

    private AionBundleFinalizer(Builder b) {
        ds = b.ds;
        api = b.api;
        tipState = b.tipState;
        contractAddress = b.contractAddress;

        haltDelaySeconds = b.haltDelaySeconds;
        finalityDepth = b.finalityDepth;
    }

    @Override
    public void run() {
        log.info("AionBundleFinalizer Started ...");

        int errAccumulator = 0;
        while (!shutdown && errAccumulator < MAX_CONSECUTIVE_ERR) {
            try {
                // Get last finalized bundle id's
                Optional<Long> aionFinalizedBundleId = ds.getAionFinalizedBundleId();
                Optional<Long> ethFinalizedBundleId = ds.getEthFinalizedBundleId();

                // Eth processor hasn't kicked in yet
                if (!ethFinalizedBundleId.isPresent()) {
                    log.info("Ethereum History is empty. Sleeping {}s.", haltDelaySeconds);
                    TimeUnit.SECONDS.sleep(haltDelaySeconds);
                    continue;
                }

                // Maybe the Eth side has not caught up yet?
                if (aionFinalizedBundleId.isPresent()) {
                    int compare = aionFinalizedBundleId.get().compareTo(ethFinalizedBundleId.get());

                    // if AionBundleId > EthBundleId, we have a problem (since Aion only follows the Eth database)
                    if (compare > 0) {
                        throw new IllegalStateException("Aion Finalized Bundle Id [" + aionFinalizedBundleId.get() + "] found " +
                                "to be greater than Eth Finalized Bundle Id [" + ethFinalizedBundleId.get() + "]");
                    }
                    else if (compare == 0) {
                        log.info("Aion all caught up to Ethereum History. EthFinalized[{}] == AionFinalized[{}]. Sleeping {}s.",
                                ethFinalizedBundleId.get(), aionFinalizedBundleId.get(), haltDelaySeconds);
                        TimeUnit.SECONDS.sleep(haltDelaySeconds);
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

                if (end < start) {
                    throw new IllegalStateException("Inconsistent DB state; Eth Finalized Bundle Id=" + end +
                            " cannot be less than Eth Finalized Bundle Id=" + start);
                }

                Optional<List<PersistentBundle>> observedEthBundles = ds.getBundleRangeClosed(start, end);
                if (!observedEthBundles.isPresent() || observedEthBundles.get().size() == 0) {
                    throw new IllegalStateException("Inconsistent DB state; Eth Finalized Bundle table does not " +
                            "contain bundleId range: [" + start + " - " + end + "]");
                }

                List<PersistentBundle> bundles = observedEthBundles.get();
                bundles.sort(PersistentBundle::compareTo);
                if (!isBundleListIntegrityOK(bundles)) {
                    throw new IllegalStateException("Inconsistent DB state; Bundle list not contiguous");
                }

                log.info("Non-finalized bundles found on Ethereum: [{} - {}]", start, end);

                Iterator<PersistentBundle> iterator = bundles.iterator();
                PersistentBundle poll = iterator.next();

                // Ok, now poll Aion if they've seen the bundles we just saw on Ethereum
                while (!shutdown) {
                    log.info("Bundle Id=[{}] Start processing", poll.getBundleId());

                    // Determine blockId with finalization (tipState - finalizationDepth)
                    Optional<Long> latestBlockNumber = tipState.getBlockNumber();
                    if (!latestBlockNumber.isPresent()) {
                        log.info("AionTipState not started yet. Sleeping {}s", haltDelaySeconds);
                        TimeUnit.SECONDS.sleep(haltDelaySeconds);
                        continue;
                    }
                    long finalizedBlock = Long.max(latestBlockNumber.get() - finalityDepth, 0L);

                    log.info("Bundle Id=[{}] Querying ATB contract ...", poll.getBundleId());

                    // Ask ATB contract if it's seen the bundle, if not just keep polling for bundleHash
                    Word32 bundleHash = poll.getBundle().getBundleHash();

                    Stopwatch timer = Stopwatch.createStarted();
                    Word32 transactionHash = new Word32(api.contractCall(contractAddress, actionMap(bundleHash), Long.toString(finalizedBlock)));
                    log.info("Bundle Id=[{}] ATB query returned in {}", poll.getBundleId(), timer.stop().toString());

                    if (transactionHash.equals(Word32.EMPTY)) {
                        log.info("No ATB Transfer for bundleHash=[{}]. Sleeping {}s", bundleHash, haltDelaySeconds);
                        TimeUnit.SECONDS.sleep(haltDelaySeconds);
                        continue;
                    }

                    log.info("Bundle Id=[{}] Querying for receipt ...", poll.getBundleId());

                    // ATB contract returns the Aion transaction hash, go ahead and retrieve that transaction
                    timer.reset().start();
                    Optional<AionReceipt> receipt = api.getReceipt(transactionHash);
                    if (!receipt.isPresent()) {
                        throw new IllegalStateException("Aion kernel could not find Aion Transaction Hash: "+
                                transactionHash.toStringWithPrefix());
                    }

                    // Ok to write the bundle to DB now.
                    StatefulBundle sb = getFinalizedStatefulBundle(poll, receipt.get());
                    /**
                     * It's OK that this is not the right aionFinalizedBlockNumber, since in this logic we don't depend
                     * on that number.
                     */
                    ChainLink aionChainTip = new ChainLink(sb.getAionReceipt().getBlockNumber(), sb.getAionReceipt().getBlockHash());
                    log.info("Bundle Id=[{}] Receipt query returned in {}. Aion BN=[{}]", poll.getBundleId(),
                            timer.stop().toString(), aionChainTip.getNumber());

                    ds.storeAionFinalizedBundles(List.of(sb), aionChainTip, sb);

                    if (iterator.hasNext() && !shutdown) {
                        poll = iterator.next();
                        continue;
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Interruption detected by AionBundleFinalizer");
                    }

                    break;
                }

                errAccumulator = 0; // since all went well
            }
            catch (MalformedApiResponseException | IncompleteApiCallException | QuorumNotAvailableException e) {
                // checked exceptions we might be able to recover from if we re-try
                try {
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
                log.error("Caught a PersistenceServiceException. Crashing the process");
                throw new RuntimeException(e.getCause());
            }

            // all other exceptions, let them just crash the process

        } // Top-Level While Loop

        if (errAccumulator >= MAX_CONSECUTIVE_ERR) {
            throw new RuntimeException("Exited due to [errAccumulator >= MAX_CONSECUTIVE_ERR]");
        }

        log.info("Exiting gracefully ...");
    }

    public void shutdown() {
        this.shutdown = true;
        log.info("Shutdown signal received");
    }

    private static boolean isBundleListIntegrityOK(List<PersistentBundle> bundles) {
        PersistentBundle previous = null;
        for (PersistentBundle b : bundles) {
            if (b == null) return false;

            if (previous == null) {
                previous = b;
                continue;
            }

            if (previous.getBundleId() + 1 != b.getBundleId()) return false;

            previous = b;
        }

        return true;
    }

    private static ImmutableBytes actionMap(Word32 bundleHash) {
        return new ImmutableBytes(new FvmAbiCodec("actionMap(bytes32)", new FvmBytes32(bundleHash)).encode());
    }

    private static StatefulBundle getFinalizedStatefulBundle(PersistentBundle b, AionReceipt r) {
        StatefulBundle sb = new StatefulBundle(b);
        sb.setStored();
        sb.setSigned(null);
        sb.setSubmitted(null);
        sb.setSealed(r);
        sb.setFinalized();
        return sb;
    }
}
