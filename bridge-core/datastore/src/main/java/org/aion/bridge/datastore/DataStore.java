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

package org.aion.bridge.datastore;

import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.base.types.Word16;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.AionFinalizedBundle;
import org.aion.bridge.chain.bridge.PersistentBundle;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.eth.types.EthAddress;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @implNote fail cases communicated via checked exceptions
 */
public interface DataStore {

    // writes

    void storeEthFinalizedBundles(List<StatefulBundle> bundles, ChainLink ethChainTip,
                                  StatefulBundle finalizationTip, Map<Word32, EthAddress> txHashToEthAddressMap)
            throws PersistenceServiceException;
    void storeEthChainHistory(ChainLink ethChainTop)
            throws PersistenceServiceException;
    void storeAionFinalizedBundles(List<StatefulBundle> bundles, ChainLink aionChainTip,
                                   StatefulBundle finalizationTip) throws PersistenceServiceException;
    void storeAionChainHistory(ChainLink aionChainTip)
            throws PersistenceServiceException;

    void storeAionLatestBlock(Long aionLatestBlockNumber)
            throws PersistenceServiceException;
    void storeAionEntityBalance(BigInteger bridgeBalance, BigInteger relayerBalance, long blockNumber)
            throws PersistenceServiceException;

    // reads
    Optional<List<PersistentBundle>> getBundleRangeClosed(Long startBundleId, Long endBundleId)
            throws PersistenceServiceException;
    Optional<PersistentBundle> getEthFinalizedBundle() throws PersistenceServiceException;
    Optional<Long> getEthFinalizedBundleId() throws PersistenceServiceException;
    Optional<ChainLink> getEthFinalizedBlock() throws PersistenceServiceException;
    Optional<Long> getAionFinalizedBundleId() throws PersistenceServiceException;
    Optional<ChainLink> getAionFinalizedBlock() throws PersistenceServiceException;
    Optional<Timestamp> getEthBundleCreationTimestamp(long bundleId) throws PersistenceServiceException;

    Optional<List<AionFinalizedBundle>> getAionTxHashRangeClosed(Long startBundleId, Long endBundleId) throws PersistenceServiceException;
    Optional<List<Word16>> getTransferValueRangeClosed(Long startBundleId, Long endBundleId) throws PersistenceServiceException;

    }
