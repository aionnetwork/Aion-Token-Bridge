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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.base.types.Word16;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.ByteUtils;
import org.aion.bridge.chain.bridge.*;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;

@SuppressWarnings("Duplicates")
public class MySqlDatastore implements DataStore {

    private static final Logger log = LoggerFactory.getLogger(MySqlDatastore.class);
    private final static ObjectMapper jackson = new ObjectMapper();
    private final DbConnectionManager ds;

    public MySqlDatastore(@Nonnull DbConnectionManager ds) {
        this.ds = ds;
    }

    private void executeBatchUpdate(PreparedStatement ps, int rowCount) throws SQLException {
        int[] updates = ps.executeBatch();

        if (updates.length != rowCount)
            throw new IllegalStateException("MySqlDatastore.executeBatchUpdate: updates.length != rowCount");
        for (int i : updates) {
            if (i != 1) {
                throw new IllegalStateException("MySqlDatastore.executeBatchUpdate: i != 1");
            }
        }
    }

    /**
     * Similar to executeBatchUpdate however runs for replacements which may return 1 if update or 2 if replace
     * @@implNote https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
     */
    private void executeBatchReplace(PreparedStatement ps, int rowCount) throws SQLException {
        int[] updates = ps.executeBatch();

        if (updates.length != rowCount)
            throw new IllegalStateException("MySqlDatastore.executeBatchReplace: updates.length != rowCount");
        for (int i : updates) {
            if (i != 1 && i != 2) {
                throw new IllegalStateException("MySqlDatastore.executeBatchReplace: i != 1 && i != 2");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------

    @Override
    public void storeEthFinalizedBundles(List<StatefulBundle> bundles, ChainLink ethChainTip,
                                         StatefulBundle finalizationTip, Map<Word32, EthAddress> txHashToEthAddressMap)
            throws PersistenceServiceException {

        /**
         * @ImplNote Not using try-with-resources due to inner statement needing connection to roleback
         */
        try(Connection c = ds.getConnection()) {
            try (PreparedStatement psBundle = c.prepareStatement(MySqlQuery.INSERT_ETH_FINALIZED_BUNDLE);
                 PreparedStatement psTransfers = c.prepareStatement(MySqlQuery.INSERT_ETH_TRANSFER);
                 PreparedStatement psBundleStatus = c.prepareStatement(MySqlQuery.UPDATE_ETH_FINALIZED_BUNDLE_ID);
                 PreparedStatement psChainTip = c.prepareStatement(MySqlQuery.UPDATE_ETH_FINALIZED_BLOCK)) {

                if (c.getAutoCommit())
                    throw new IllegalStateException("auto-commit must be OFF for this operation.");

                int transferCount = 0;

                for (StatefulBundle sb : bundles) {
                    if (sb.getState() != StatefulBundle.State.BUNDLED)
                        throw new IllegalStateException("stateful bundle should be in state BUNDLED");

                    Bundle b = sb.getBundle();
                    List<Transfer> transfers = b.getTransfers();

                    psBundle.setLong(1, sb.getBundleId());
                    psBundle.setString(2, b.getBundleHash().toString());
                    psBundle.setLong(3, b.getEthBlockNumber());
                    psBundle.setString(4, b.getEthBlockHash().toString());
                    psBundle.setInt(5, b.getIndexInEthBlock());
                    psBundle.setString(6, jackson.writeValueAsString(transfers));
                    psBundle.addBatch();

                    for (Transfer transfer : transfers) {
                        EthAddress ethAddress = txHashToEthAddressMap.get(transfer.getEthTxHash());
                        if (ethAddress == null)
                            throw new IllegalStateException("MySqlDatastore - could not find Eth Tx Hash in txHashToEthAddressMap");

                        psTransfers.setString(1, transfer.getEthTxHash().toString());
                        psTransfers.setLong(2, sb.getBundleId());
                        psTransfers.setString(3, sb.getBundleHash().toString());
                        psTransfers.setString(4, ethAddress.toString());
                        psTransfers.setString(5, transfer.getAionAddress().toString());
                        psTransfers.setString(6, transfer.getAionTransferAmount().toString());
                        psTransfers.addBatch();

                        transferCount++;
                    }
                }

                psBundleStatus.setLong(1, finalizationTip.getBundleId());
                psBundleStatus.setString(2, finalizationTip.getBundleHash().toString());
                psBundleStatus.addBatch();

                psChainTip.setLong(1, ethChainTip.getNumber());
                psChainTip.setString(2, ethChainTip.getHash().toString());
                psChainTip.addBatch();

                executeBatchUpdate(psBundle, bundles.size());
                executeBatchUpdate(psTransfers, transferCount);
                executeBatchReplace(psBundleStatus, 1);
                executeBatchReplace(psChainTip, 1);

                c.commit();
            }
            catch (IOException e) {
                log.error("Jackson failed to serialized transactions to Json", e);
                throw new PersistenceServiceException(e);
            }
            catch (SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException f) {
                    log.error("Failed to rollback commit on SQLException", e);
                    throw new PersistenceServiceException(f);
                }
                log.error("SQLException caught; Commit rolled back successfully", e);
                throw new PersistenceServiceException(e);
            }

        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public void storeEthChainHistory(ChainLink ethChainTip) throws PersistenceServiceException {

        try(Connection c = ds.getConnection()) {
            try (PreparedStatement psChainTip = c.prepareStatement(MySqlQuery.UPDATE_ETH_FINALIZED_BLOCK)) {
                if (c.getAutoCommit())
                    throw new IllegalStateException("auto-commit must be OFF for this operation.");

                psChainTip.setLong(1, ethChainTip.getNumber());
                psChainTip.setString(2, ethChainTip.getHash().toString());
                psChainTip.addBatch();

                executeBatchReplace(psChainTip, 1);

                c.commit();
            } catch (SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException f) {
                    log.error("Failed to rollback commit on SQLException", e);
                    throw new PersistenceServiceException(f);
                }
                log.error("SQLException caught; Commit rolled back successfully", e);
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public void storeAionChainHistory(ChainLink aionChainTip) throws PersistenceServiceException {
        try(Connection c = ds.getConnection()) {
            try (PreparedStatement psChainTip = c.prepareStatement(MySqlQuery.UPDATE_AION_FINALIZED_BLOCK)) {
                if (c.getAutoCommit())
                    throw new IllegalStateException("auto-commit must be OFF for this operation.");

                psChainTip.setLong(1, aionChainTip.getNumber());
                psChainTip.setString(2, aionChainTip.getHash().toString());
                psChainTip.addBatch();

                executeBatchReplace(psChainTip, 1);

                c.commit();
            } catch (SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException f) {
                    log.error("Failed to rollback commit on SQLException", e);
                    throw new PersistenceServiceException(f);
                }
                log.error("SQLException caught; Commit rolled back successfully", e);
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public void storeAionFinalizedBundles(List<StatefulBundle> bundles, ChainLink aionChainTip,
                                          StatefulBundle finalizationTip) throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement psBundle = c.prepareStatement(MySqlQuery.INSERT_AION_FINALIZED_BUNDLE);
                 PreparedStatement psBundleStatus = c.prepareStatement(MySqlQuery.UPDATE_AION_FINALIZED_BUNDLE_ID);
                 PreparedStatement psChainTip = c.prepareStatement(MySqlQuery.UPDATE_AION_FINALIZED_BLOCK)) {
                if (c.getAutoCommit())
                    throw new IllegalStateException("Auto-commit must be ON for this operation.");

                for (StatefulBundle sb : bundles) {
                    if (sb.getState() != StatefulBundle.State.FINALIZED)
                        throw new IllegalStateException("Stateful bundle should be in state FINALIZED");

                    AionReceipt r = sb.getAionReceipt();

                    psBundle.setLong(1, sb.getBundleId());
                    psBundle.setString(2, sb.getBundleHash().toString());
                    psBundle.setString(3, r.getTransactionHash().toString());
                    psBundle.setLong(4, r.getBlockNumber());
                    psBundle.setString(5, r.getBlockHash().toString());
                    psBundle.addBatch();
                }

                psBundleStatus.setLong(1, finalizationTip.getBundleId());
                psBundleStatus.setString(2, finalizationTip.getBundleHash().toString());
                psBundleStatus.addBatch();

                psChainTip.setLong(1, aionChainTip.getNumber());
                psChainTip.setString(2, aionChainTip.getHash().toString());
                psChainTip.addBatch();

                executeBatchUpdate(psBundle, bundles.size());
                executeBatchReplace(psBundleStatus, 1);
                executeBatchReplace(psChainTip, 1);

                c.commit();
            } catch (SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException f) {
                    log.error("Failed to rollback commit on SQLException", e);
                    throw new PersistenceServiceException(f);
                }
                log.error("SQLException caught; Commit rolled back successfully", e);
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public void storeAionLatestBlock(Long aionLatestBlockNumber) throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement psAionTip = c.prepareStatement(MySqlQuery.UPDATE_AION_LATEST_BLOCK_NUMBER)) {

                psAionTip.setLong(1, aionLatestBlockNumber);
                psAionTip.addBatch();

                executeBatchReplace(psAionTip, 1);

                c.commit();
            } catch (SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException f) {
                    log.error("Failed to rollback commit on SQLException", e);
                    throw new PersistenceServiceException(f);
                }
                log.error("SQLException caught; Commit rolled back successfully", e);
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public void storeAionEntityBalance(BigInteger bridgeBalance, BigInteger relayerBalance, long blockNumber)
            throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement psBridgeBalance = c.prepareStatement(MySqlQuery.UPDATE_AION_CONTRACT_BALANCE);
                 PreparedStatement psRelayerBalance = c.prepareStatement(MySqlQuery.UPDATE_AION_RELAYER_BALANCE)) {
                if (c.getAutoCommit())
                    throw new IllegalStateException("Auto-commit must be OFF for this operation.");

                String _bridgeBalance = (new Word32(ByteUtils.pad(bridgeBalance.toByteArray(), 32))).toString();
                String _relayerBalance = (new Word32(ByteUtils.pad(relayerBalance.toByteArray(), 32))).toString();

                psBridgeBalance.setString(1, _bridgeBalance);
                psBridgeBalance.setLong(2, blockNumber);
                psBridgeBalance.addBatch();

                psRelayerBalance.setString(1, _relayerBalance);
                psRelayerBalance.setLong(2, blockNumber);
                psRelayerBalance.addBatch();

                executeBatchReplace(psBridgeBalance, 1);
                executeBatchReplace(psRelayerBalance, 1);

                c.commit();
            } catch (SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException f) {
                    log.error("Failed to rollback commit on SQLException", e);
                    throw new PersistenceServiceException(f);
                }
                log.error("SQLException caught; Commit rolled back successfully", e);
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------
    @Override
    public Optional<List<PersistentBundle>> getBundleRangeClosed(Long startBundleId, Long endBundleId)
            throws PersistenceServiceException {

        if (startBundleId < 0 || endBundleId < 0) throw new IllegalArgumentException("startBundleId || endBundleId < 0");
        if (startBundleId > endBundleId) throw new IllegalArgumentException("startBundleId > endBundleId");

        Set<Long> expectedBundleIds = LongStream.rangeClosed(startBundleId, endBundleId).boxed().collect(toSet());
        int expectedListSize = expectedBundleIds.size();

        List<PersistentBundle> result = new ArrayList<>();

        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_ETH_FINALIZED_BUNDLE_RANGE)) {

                ps.setLong(1, startBundleId);
                ps.setLong(2, endBundleId);

                try (ResultSet rs = ps.executeQuery()) {
                    rs.last();
                    int size = rs.getRow();
                    if (size != expectedListSize) {
                        c.commit();
                        return Optional.empty();
                    }

                    rs.beforeFirst();
                    while (rs.next()) {
                        Long bundleId = rs.getLong(1);
                        String bundleHash = rs.getString(2);
                        Long ethBlockNumber = rs.getLong(3);
                        String ethBlockHash = rs.getString(4);
                        Integer indexInEthBlock = rs.getInt(5);
                        String transfersStr = rs.getString(6);

                        if (!allNotNull(bundleId, bundleHash, ethBlockNumber, ethBlockHash, indexInEthBlock, transfersStr))
                            throw new IllegalStateException("database in inconsistent state");

                        Bundle bundle = new Bundle(ethBlockNumber, new Word32(ethBlockHash), indexInEthBlock,
                                jackson.readValue(transfersStr, new TypeReference<List<Transfer>>() {
                                }));

                        result.add(new PersistentBundle(bundleId, bundle));
                        expectedBundleIds.remove(bundleId);
                    }
                }

                if (expectedBundleIds.size() != 0 && result.size() != expectedListSize)
                    throw new IllegalStateException("missing bundle ids from database");
                c.commit();

                return Optional.of(result);

            } catch (SQLException | IOException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public Optional<PersistentBundle> getEthFinalizedBundle() throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_STATUS_ETH_FINALIZED_BUNDLE_FULL);
                 ResultSet rs = ps.executeQuery()) {

                rs.last();
                int size = rs.getRow();
                if (size == 0) {
                    c.commit();
                    return Optional.empty();
                }

                if (size > 1)
                    throw new IllegalStateException("result set size cannot be > 1");

                // bundle_id, bundle_hash, eth_block_number, eth_block_hash, index_in_eth_block, transfers

                Long bundleId = rs.getLong(1);
                String bundleHash = rs.getString(2);
                Long ethBlockNumber = rs.getLong(3);
                String ethBlockHash = rs.getString(4);
                Integer indexInEthBlock = rs.getInt(5);
                String transfersStr = rs.getString(6);

                if (!allNotNull(bundleId, bundleHash, ethBlockNumber, ethBlockHash, indexInEthBlock, transfersStr))
                    throw new IllegalStateException("database in inconsistent state");

                Bundle bundle = new Bundle(ethBlockNumber, new Word32(ethBlockHash), indexInEthBlock,
                        jackson.readValue(transfersStr, new TypeReference<List<Transfer>>() {
                        }));

                c.commit();
                return Optional.of(new PersistentBundle(bundleId, bundle));

            } catch (SQLException | IOException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public Optional<Long> getEthFinalizedBundleId() throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_STATUS_ETH_FINALIZED_BUNDLE_ID);
                 ResultSet rs = ps.executeQuery()) {

                rs.last();
                int size = rs.getRow();
                if (size == 0) {
                    c.commit();
                    return Optional.empty();
                }
                if (size > 1)
                    throw new IllegalStateException("database in inconsistent state");

                Long bundleId = rs.getLong(1);

                if (!allNotNull(bundleId))
                    throw new IllegalStateException("database in inconsistent state");

                c.commit();

                return Optional.of(bundleId);

            } catch (SQLException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public Optional<Long> getAionFinalizedBundleId() throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_STATUS_AION_FINALIZED_BUNDLE_ID);
                 ResultSet rs = ps.executeQuery()) {

                rs.last();
                int size = rs.getRow();
                if (size == 0) {
                    c.commit();
                    return Optional.empty();
                }
                if (size > 1)
                    throw new IllegalStateException("database in inconsistent state");

                Long bundleId = rs.getLong(1);

                if (!allNotNull(bundleId))
                    throw new IllegalStateException("database in inconsistent state");

                c.commit();

                return Optional.of(bundleId);

            } catch (SQLException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public Optional<ChainLink> getAionFinalizedBlock() throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_STATUS_AION_FINALIZED_BLOCK);
                 ResultSet rs = ps.executeQuery()) {

                rs.last();
                int size = rs.getRow();
                if (size == 0) {
                    c.commit();
                    return Optional.empty();
                }
                if (size > 1)
                    throw new IllegalStateException("database in inconsistent state");

                Long blockNumber = rs.getLong(1);
                String blockHash = rs.getString(2);

                if (!allNotNull(blockNumber, blockHash))
                    throw new IllegalStateException("database in inconsistent state");

                c.commit();

                return Optional.of(new ChainLink(blockNumber, new Word32(blockHash)));
            } catch (SQLException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public Optional<Timestamp> getEthBundleCreationTimestamp(long bundleId) throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_ETH_BUNDLE_CREATION_TIMESTAMP)) {

                ps.setLong(1, bundleId);
                ResultSet rs = ps.executeQuery();

                rs.last();
                int size = rs.getRow();
                if (size == 0) {
                    c.commit();
                    return Optional.empty();
                }
                if (size > 1)
                    throw new IllegalStateException("database in inconsistent state");


                Timestamp timeStamp = rs.getTimestamp(1);

                if(!allNotNull(timeStamp))
                    throw new IllegalStateException("database in inconsistent state");

                rs.close();

                c.commit();
                return Optional.of(timeStamp);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public Optional<ChainLink> getEthFinalizedBlock() throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_STATUS_ETH_FINALIZED_BLOCK);
                 ResultSet rs = ps.executeQuery()) {

                rs.last();
                int size = rs.getRow();
                if (size == 0) {
                    c.commit();
                    return Optional.empty();
                }
                if (size > 1)
                    throw new IllegalStateException("database in inconsistent state");

                Long blockNumber = rs.getLong(1);
                String blockHash = rs.getString(2);

                if (!allNotNull(blockNumber, blockHash))
                    throw new IllegalStateException("database in inconsistent state");

                c.commit();

                return Optional.of(new ChainLink(blockNumber, new Word32(blockHash)));

            } catch (SQLException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public Optional<List<AionFinalizedBundle>> getAionTxHashRangeClosed(Long startBundleId, Long endBundleId) throws PersistenceServiceException {
        if (startBundleId < 0 || endBundleId < 0) throw new IllegalArgumentException("startBundleId || endBundleId < 0");
        if (startBundleId > endBundleId) throw new IllegalArgumentException("startBundleId > endBundleId");

        Set<Long> expectedBundleIds = LongStream.rangeClosed(startBundleId, endBundleId).boxed().collect(toSet());
        int expectedListSize = expectedBundleIds.size();

        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_AION_FINALIZED_BUNDLE_MAPPING)) {

                ps.setLong(1, startBundleId);
                ps.setLong(2, endBundleId);

                List<AionFinalizedBundle> result = new ArrayList<>();

                try (ResultSet rs = ps.executeQuery()) {
                    rs.last();
                    int size = rs.getRow();
                    if (size != expectedListSize) {
                        c.commit();
                        return Optional.empty();
                    }

                    rs.beforeFirst();
                    while (rs.next()) {
                        Long bundleId = rs.getLong(1);
                        String bundleHash = rs.getString(2);
                        String aionTxHash = rs.getString(3);
                        Long aionBlockNum = rs.getLong(4);
                        String aionBlockHash = rs.getString(5);

                        if (!allNotNull(bundleId, bundleHash, aionTxHash, aionBlockNum, aionBlockHash))
                            throw new IllegalStateException("database in inconsistent state");

                        result.add(new AionFinalizedBundle(bundleId, new Word32(bundleHash), new Word32(aionTxHash),
                                aionBlockNum, new Word32(aionBlockHash)));
                        expectedBundleIds.remove(bundleId);
                    }
                }

                if (expectedBundleIds.size() != 0 && result.size() != expectedListSize)
                    throw new IllegalStateException("missing bundle ids from database");
                c.commit();

                return Optional.of(result);

            } catch (SQLException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }

    @Override
    public Optional<List<Word16>> getTransferValueRangeClosed(Long startBundleId, Long endBundleId) throws PersistenceServiceException {

        if (startBundleId < 0 || endBundleId < 0)
            throw new IllegalArgumentException("startBlockNumber || endBlockNumber < 0");
        if (startBundleId > endBundleId) throw new IllegalArgumentException("startBlockNumber > endBlockNumber");

        List<Word16> result = new ArrayList<>();

        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(MySqlQuery.GET_TRANSFER_VALUE_IN_BLOCK_RANGE)) {

                ps.setLong(1, startBundleId);
                ps.setLong(2, endBundleId);

                try (ResultSet rs = ps.executeQuery()) {

                    rs.beforeFirst();
                    while (rs.next()) {
                        String transferValue = rs.getString(1);
                        result.add(new Word16(transferValue));
                    }
                }
                c.commit();

                return Optional.of(result);

            } catch (SQLException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain a datasource connection", e);
            throw new PersistenceServiceException(e);
        }
    }
}
