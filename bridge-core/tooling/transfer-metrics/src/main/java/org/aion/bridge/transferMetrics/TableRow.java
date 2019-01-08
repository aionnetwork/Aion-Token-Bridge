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

package org.aion.bridge.transferMetrics;

import org.aion.bridge.chain.base.types.Word32;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TableRow {
    private Word32 txHash;
    private long ethTimestamp;
    private long aionTimestamp;
    private long totalDuration;
    private BigDecimal amount;

    public Word32 getTxHash() {
        return txHash;
    }

    public long getEthTimestamp() {
        return ethTimestamp;
    }

    public long getAionTimestamp() {
        return aionTimestamp;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TableRow(Word32 txHash, long ethTimestamp, long aionTimestamp, long totalDuration, BigDecimal amount) {

        this.txHash = txHash;
        this.ethTimestamp = ethTimestamp;
        this.aionTimestamp = aionTimestamp;
        this.totalDuration = totalDuration;
        this.amount = amount;
    }

    public List<String> toTableRow() {
        List<String> output = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

        output.add(txHash.toString());

        cal.setTimeInMillis(ethTimestamp * 1000);
        output.add(formatter.format(cal.getTime()));
        cal.clear();

        cal.setTimeInMillis(aionTimestamp * 1000);
        output.add(formatter.format(cal.getTime()));
        cal.clear();

        output.add(DurationFormatUtils.formatDuration(totalDuration,"dd:HH:mm:ss:SSS"));

        output.add(amount.toPlainString());

        return output;
    }
}