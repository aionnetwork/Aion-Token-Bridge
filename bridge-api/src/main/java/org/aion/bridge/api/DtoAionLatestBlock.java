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
import org.springframework.jdbc.core.RowMapper;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("WeakerAccess")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class DtoAionLatestBlock  {
    public static final String INTEGRITY_KEEPER = "status";

    public static final DtoAionLatestBlock EMPTY = new DtoAionLatestBlock(new Aion.Builder().build());

    public static class Aion {
        private static class Builder {
            BigInteger latestBlockNumber = null;
            Builder setLatestBlockNumber(BigInteger x) { latestBlockNumber = x; return this; }
            Aion build() { return new Aion(this); }
        }

        private final BigInteger latestBlockNumber;

        private Aion(Builder b) {
            this.latestBlockNumber = b.latestBlockNumber;
        }

        public BigInteger getLatestBlockNumber() { return latestBlockNumber; }
    }

    private final Aion aion;

    public DtoAionLatestBlock(Aion aion) {
        this.aion = aion;
    }

    public static final String QUERY = "select aion_block_number as aion_latest_block " +
            "from status_aion_latest_block " +
            "where integrity_keeper = '"+INTEGRITY_KEEPER+"';";

    public static class JdbcRowMapper implements RowMapper<DtoAionLatestBlock> {
        @Override
        public DtoAionLatestBlock mapRow(ResultSet rs, int rowNum) throws SQLException {
            BigInteger aionLatestBlock = (BigInteger) rs.getObject("aion_latest_block");

            return new DtoAionLatestBlock(new Aion.Builder()
                            .setLatestBlockNumber(aionLatestBlock)
                            .build());
        }
    }

    public Aion getAion() { return aion; }
}

