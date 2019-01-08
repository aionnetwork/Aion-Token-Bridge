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
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("WeakerAccess")
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.PUBLIC_ONLY, setterVisibility = Visibility.NONE)
public class DtoFinalizationStatus  {
    public static final String INTEGRITY_KEEPER = "status";

    public static final DtoFinalizationStatus EMPTY = new DtoFinalizationStatus(new Eth.Builder().build(), new Aion.Builder().build());

    public static class Eth {
        private static class Builder {
            BigInteger finalizedBundleId = null;
            BigInteger finalizedBlockNumber = null;

            Builder setFinalizedBundleId(BigInteger x) { finalizedBundleId = x; return this; }
            Builder setFinalizedBlockNumber(BigInteger x) { finalizedBlockNumber = x; return this; }

            Eth build() { return new Eth(this); }
        }

        private final BigInteger finalizedBlockNumber;
        private final BigInteger finalizedBundleId;

        private Eth(Builder b) {
            this.finalizedBlockNumber = b.finalizedBlockNumber;
            this.finalizedBundleId = b.finalizedBundleId;
        }

        public BigInteger getFinalizedBlockNumber() { return finalizedBlockNumber; }
        public BigInteger getFinalizedBundleId() { return finalizedBundleId; }
    }

    public static class Aion {
        private static class Builder {
            BigInteger latestBlockNumber = null;
            BigInteger finalizedBundleId = null;
            BigInteger finalizedBlockNumber = null;

            Builder setLatestBlockNumber(BigInteger x) { latestBlockNumber = x; return this; }
            Builder setFinalizedBundleId(BigInteger x) { finalizedBundleId = x; return this; }
            Builder setFinalizedBlockNumber(BigInteger x) { finalizedBlockNumber = x; return this; }

            Aion build() { return new Aion(this); }
        }

        private final BigInteger latestBlockNumber;
        private final BigInteger finalizedBundleId;
        private final BigInteger finalizedBlockNumber;

        private Aion(Builder b) {
            this.latestBlockNumber = b.latestBlockNumber;
            this.finalizedBundleId = b.finalizedBundleId;
            this.finalizedBlockNumber = b.finalizedBlockNumber;
        }

        public BigInteger getLatestBlockNumber() { return latestBlockNumber; }
        public BigInteger getFinalizedBundleId() { return finalizedBundleId; }
        public BigInteger getFinalizedBlockNumber() { return finalizedBlockNumber; }
    }

    private final Eth eth;
    private final Aion aion;

    public DtoFinalizationStatus(Eth eth, Aion aion) {
        this.eth = eth;
        this.aion = aion;
    }

    public static final String QUERY =
            "select a.integrity_keeper as integrity_keeper, " +
                    "a.eth_block_number as eth_finalized_block, " +
                    "b.bundle_id as eth_finalized_bundle, " +
                    "c.aion_block_number as aion_latest_block, " +
                    "d.aion_block_number as aion_finalized_block, " +
                    "e.bundle_id as aion_finalized_bundle " +

                    "from (select * from status_eth_finalized_block where integrity_keeper = '"+INTEGRITY_KEEPER+"') as a " +
                    "left join status_eth_finalized_bundle as b on a.integrity_keeper = b.integrity_keeper " +
                    "left join status_aion_latest_block as c on a.integrity_keeper = c.integrity_keeper " +
                    "left join status_aion_finalized_block as d on a.integrity_keeper = d.integrity_keeper " +
                    "left join status_aion_finalized_bundle as e on a.integrity_keeper = e.integrity_keeper;";

    public static class JdbcRowMapper implements RowMapper<DtoFinalizationStatus> {
        @Override
        public DtoFinalizationStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
            BigInteger ethFinalizedBlock = (BigInteger) rs.getObject("eth_finalized_block");
            BigInteger ethFinalizedBundle = (BigInteger) rs.getObject("eth_finalized_bundle");
            BigInteger aionLatestBlock = (BigInteger) rs.getObject("aion_latest_block");
            BigInteger aionFinalizedBlock = (BigInteger) rs.getObject("aion_finalized_block");
            BigInteger aionFinalizedBundle = (BigInteger) rs.getObject("aion_finalized_bundle");

            return new DtoFinalizationStatus(
                    new Eth.Builder()
                            .setFinalizedBlockNumber(ethFinalizedBlock)
                            .setFinalizedBundleId(ethFinalizedBundle)
                            .build(),
                    new Aion.Builder()
                            .setFinalizedBlockNumber(aionFinalizedBlock)
                            .setFinalizedBundleId(aionFinalizedBundle)
                            .setLatestBlockNumber(aionLatestBlock)
                            .build());
        }
    }

    public Eth getEth() { return eth; }
    public Aion getAion() { return aion; }
}
