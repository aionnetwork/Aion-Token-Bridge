/*
 *
 *   This code is licensed under the MIT License
 *
 *   Copyright (c) 2019 Aion Foundation https://aion.network/
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 *
 */

package org.aion.bridge.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class DtoEthTransactionState {

    public final static DtoEthTransactionState UNSUPPORTED = new DtoEthTransactionState(BundleState.UNSUPPORTED);
    public final static DtoEthTransactionState NOT_FOUND = new DtoEthTransactionState(BundleState.NOT_FOUND);

    public enum BundleState {
        UNSUPPORTED, // state cannot be determined

        NOT_FOUND, // eth transaction not picked up by bridge yet

        STORED, // bundle stored in db
        SUBMITTED // bundle sealed in a destination block
    }

    private final BundleState state;
    private final AionInfo aionInfo;
    private final EthInfo ethInfo;

    public static class AionInfo {
        private static class Builder {
            String aionTxHash;
            String aionBlockHash;
            BigInteger aionBlockNumber;

            Builder setAionTxHash(String x) { aionTxHash = x; return this; }
            Builder setAionBlockHash(String x) { aionBlockHash = x; return this; }
            Builder setAionBlockNumber(BigInteger x) { aionBlockNumber = x; return this; }

            AionInfo build() {
                if (ObjectUtils.allNotNull(aionTxHash, aionBlockHash, aionBlockNumber))
                    return new AionInfo(this);

                throw new IllegalArgumentException("Missing one of required parameters");
            }
        }

        private final String aionTxHash;
        private final String aionBlockHash;
        private final BigInteger aionBlockNumber;

        private AionInfo(Builder b) {
            this.aionTxHash = b.aionTxHash;
            this.aionBlockHash = b.aionBlockHash;
            this.aionBlockNumber = b.aionBlockNumber;
        }

        public String getAionTxHash() { return aionTxHash; }
        public String getAionBlockHash() { return aionBlockHash; }
        public BigInteger getAionBlockNumber() { return aionBlockNumber; }
    }

    public static class EthInfo {
        private static class Builder {
            BigInteger bundleId;
            String bundleHash;

            String ethTxHash;
            String ethBlockHash;
            BigInteger ethBlockNumber;
            String ethAddress;
            String aionTransferAmount;
            String aionAddress;

            Builder setBundleId(BigInteger x) { bundleId = x; return this; }
            Builder setBundleHash(String x) { bundleHash = x; return this; }

            Builder setEthTxHash(String x) { ethTxHash = x; return this; }
            Builder setEthBlockHash(String x) { ethBlockHash = x; return this; }
            Builder setEthBlockNumber(BigInteger x) { ethBlockNumber = x; return this; }
            Builder setEthAddress(String x) { ethAddress = x; return this; }
            Builder setAionTransferAmount(String x) { aionTransferAmount = x; return this; }
            Builder setAionAddress(String x) { aionAddress = x; return this; }

            EthInfo build() {
                if (ObjectUtils.allNotNull(bundleId, bundleHash, ethTxHash, ethBlockHash, ethBlockNumber, ethAddress, aionTransferAmount, aionAddress))
                    return new EthInfo(this);

                throw new IllegalArgumentException("Missing one of required parameters");
            }
        }

        private final BigInteger bundleId;
        private final String bundleHash;

        private final String ethTxHash;
        private final String ethBlockHash;
        private final BigInteger ethBlockNumber;
        private final String ethAddress;
        private final String aionTransferAmount;
        private final String aionAddress;

        private EthInfo(Builder b) {
            this.bundleId = b.bundleId;
            this.bundleHash = b.bundleHash;
            this.ethTxHash = b.ethTxHash;
            this.ethBlockHash = b.ethBlockHash;
            this.ethBlockNumber = b.ethBlockNumber;
            this.ethAddress = b.ethAddress;
            this.aionTransferAmount = b.aionTransferAmount;
            this.aionAddress = b.aionAddress;
        }

        public BigInteger getBundleId() { return bundleId; }
        public String getBundleHash() { return bundleHash; }
        public String getEthTxHash() { return ethTxHash; }
        public String getEthBlockHash() { return ethBlockHash; }
        public String getEthAddress() { return ethAddress; }
        public BigInteger getEthBlockNumber() { return ethBlockNumber; }
        public String getAionTransferAmount() { return aionTransferAmount; }
        public String getAionAddress() { return aionAddress; }
    }

    public DtoEthTransactionState(BundleState state) {
        this(state, null, null);
    }

    public DtoEthTransactionState(BundleState state, EthInfo ethInfo, AionInfo aionInfo) {
        Objects.requireNonNull(state);

        if (ethInfo == null && (state != BundleState.UNSUPPORTED && state != BundleState.NOT_FOUND))
            throw new IllegalArgumentException();

        if (aionInfo == null && (state == BundleState.SUBMITTED))
            throw new IllegalArgumentException();

        this.state = state;
        this.aionInfo = aionInfo;
        this.ethInfo = ethInfo;
    }

    public static final String QUERY =
            "select a.eth_tx_hash as eth_tx_hash, " +
                    "a.bundle_id as bundle_id, " +
                    "a.bundle_hash as eth_transfer_bundle_hash, " +
                    "a.eth_address as eth_transfer_eth_address, " +
                    "a.aion_address as eth_transfer_aion_address, " +
                    "a.aion_transfer_amount as aion_transfer_amount, " +

                    "b.bundle_hash as eth_finalized_bundle_hash, " +
                    "b.eth_block_number as eth_finalized_block_number, " +
                    "b.eth_block_hash as eth_finalized_block_hash, " +

                    "c.bundle_hash as aion_finalized_bundle_hash, " +
                    "c.aion_tx_hash as aion_finalized_tx_hash, " +
                    "c.aion_block_number as aion_finalized_block_number, " +
                    "c.aion_block_hash as aion_finalized_block_hash " +

                    "from (select * from eth_transfer where eth_tx_hash = ?) as a " +
                    "left join eth_finalized_bundle  as b on a.bundle_id = b.bundle_id " +
                    "left join aion_finalized_bundle as c on a.bundle_id = c.bundle_id;";

    public static class JdbcRowMapper implements RowMapper<DtoEthTransactionState> {
        @Override
        public DtoEthTransactionState mapRow(ResultSet rs, int rowNum) throws SQLException {

            BigInteger bundleId = (BigInteger) rs.getObject("bundle_id");
            String ethTxHash = rs.getString("eth_tx_hash");
            String ethBlockHash = rs.getString("eth_finalized_block_hash");
            BigInteger ethBlockNumber = (BigInteger) rs.getObject("eth_finalized_block_number");
            String ethAddress = rs.getString("eth_transfer_eth_address");

            String aionTransferAmount = rs.getString("aion_transfer_amount");
            String aionAddress = rs.getString("eth_transfer_aion_address");

            String aionFinalizedTxHash = rs.getString("aion_finalized_tx_hash");
            BigInteger aionFinalizedBlockNumber = (BigInteger) rs.getObject("aion_finalized_block_number");
            String aionFinalizedBlockHash = rs.getString("aion_finalized_block_hash");

            String ethTransferBundleHash = rs.getString("eth_transfer_bundle_hash");
            String ethFinalizedBundleHash = rs.getString("eth_finalized_bundle_hash");
            String aionFinalizedBundleHash = rs.getString("aion_finalized_bundle_hash");

            if (!ObjectUtils.allNotNull(bundleId, ethTxHash, ethBlockHash, ethBlockNumber, ethAddress, aionTransferAmount, aionAddress))
                return UNSUPPORTED;

            if (ethTransferBundleHash == null || !ethTransferBundleHash.equals(ethFinalizedBundleHash))
                return UNSUPPORTED;

            if (aionFinalizedBundleHash != null) {
                if (!ethTransferBundleHash.equals(aionFinalizedBundleHash)) {
                    return UNSUPPORTED;
                }
            }
            EthInfo ethInfo = new EthInfo.Builder()
                    .setBundleId(bundleId)
                    .setBundleHash(ethTransferBundleHash)
                    .setEthTxHash(ethTxHash)
                    .setEthBlockNumber(ethBlockNumber)
                    .setEthBlockHash(ethBlockHash)
                    .setEthAddress(ethAddress)
                    .setAionTransferAmount(aionTransferAmount)
                    .setAionAddress(aionAddress)
                    .build();

            if (aionFinalizedTxHash != null) {
                AionInfo aionInfo = new AionInfo.Builder()
                        .setAionTxHash(aionFinalizedTxHash)
                        .setAionBlockHash(aionFinalizedBlockHash)
                        .setAionBlockNumber(aionFinalizedBlockNumber)
                        .build();

                return new DtoEthTransactionState(BundleState.SUBMITTED, ethInfo, aionInfo);
            }

            return new DtoEthTransactionState(BundleState.STORED, ethInfo, null);
        }
    }

    public BundleState getState() { return state; }
    public AionInfo getAionInfo() { return aionInfo; }
    public EthInfo getEthInfo() { return ethInfo; }
}
