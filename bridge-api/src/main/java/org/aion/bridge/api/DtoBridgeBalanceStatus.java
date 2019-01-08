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
public class DtoBridgeBalanceStatus {
    public static final DtoBridgeBalanceStatus EMPTY = new DtoBridgeBalanceStatus(null, null);

    private final String balance;
    private final BigInteger blockNumber;

    public DtoBridgeBalanceStatus(String balance, BigInteger blockNumber) {
        this.balance = balance;
        this.blockNumber = blockNumber;
    }

    public static final String QUERY =
            "select aion_balance, aion_block_number from status_aion_balance where integrity_keeper = 'bridge';";

    public static class JdbcRowMapper implements RowMapper<DtoBridgeBalanceStatus> {
        @Override
        public DtoBridgeBalanceStatus mapRow(ResultSet rs, int rowNum) throws SQLException {

            String balance = (String) rs.getObject("aion_balance");
            BigInteger blockNumber = (BigInteger) rs.getObject("aion_block_number");

            return new DtoBridgeBalanceStatus(balance, blockNumber);
        }
    }

    public String getBalance() { return balance; }
    public BigInteger getBlockNumber() { return blockNumber; }
}
