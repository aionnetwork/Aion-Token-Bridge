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

import org.aion.bridge.chain.base.types.Word16;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.datastore.DataStore;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public class TotalCoinsTransferred {

    private DataStore ds;
    BigDecimal TOKEN_TO_COIN_DECIMAL_SHIFT = BigDecimal.TEN.pow(18);

    public TotalCoinsTransferred(DataStore ds) {
        this.ds = ds;
    }

    public BigDecimal getTotalTransferValue(Long startBundle, Long endBundle) {

        Optional<List<Word16>> valueList;
        BigDecimal sum = BigDecimal.ZERO;
        try {
            valueList = ds.getTransferValueRangeClosed(startBundle, endBundle);
            if (valueList.isPresent()) {
                for (Word16 val : valueList.get()) {
                    sum = sum.add(new BigDecimal(new BigInteger(val.getPayload())).divide(TOKEN_TO_COIN_DECIMAL_SHIFT));
                }
                System.out.println("Total transfers between bundle [" + startBundle + "," + endBundle + "]: " + sum + " Aion");
            } else {
                System.out.println("Error occurred during calculation of total transfer values.");
            }
        } catch (PersistenceServiceException e1) {
            e1.printStackTrace();
        }
        return sum;
    }
}