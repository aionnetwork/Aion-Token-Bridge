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

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class TableFormater {
    static String style =
            "<head>\n" +
                    "<style>\n" +
                    "table {\n" +
                    "    font-family: arial, sans-serif;\n" +
                    "    border-collapse: collapse;\n" +
                    "    width: 100%;\n" +
                    "}\n" +
                    "\n" +
                    "td, th {\n" +
                    "    border: 1px solid #dddddd;\n" +
                    "    text-align: left;\n" +
                    "    padding: 8px;\n" +
                    "}\n" +
                    "\n" +
                    "tr:nth-child(even) {\n" +
                    "    background-color: #dddddd;\n" +
                    "}\n" +
                    "</style>\n" +
                    "</head>";

    private TableFormater() {

    }

    public static String formatTable(String title, List<TableRow> rows, BigDecimal totalTransfer) {
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

        StringBuilder sb = new StringBuilder();

        sb.append(style);
        sb.append("<table>");
        String[] headers  = title.split(" ");
        for(String hdr : headers) {
            sb.append("<th>");
            sb.append(hdr);
            sb.append("</th>");
        }

        Calendar cal = Calendar.getInstance();

        for(TableRow tr: rows) {
            sb.append("<tr>");

            sb.append("<td>");
            sb.append(tr.getTxHash().toString());
            sb.append("</td>");

            sb.append("<td>");
            cal.setTimeInMillis(tr.getEthTimestamp() * 1000);
            sb.append(formatter.format(cal.getTime()));
            sb.append("</td>");

            sb.append("<td>");
            cal.setTimeInMillis(tr.getAionTimestamp() * 1000);
            sb.append(formatter.format(cal.getTime()));
            sb.append("</td>");

            sb.append("<td>");
            sb.append(DurationFormatUtils.formatDuration(tr.getTotalDuration(),"dd:HH:mm:ss:SSS"));
            sb.append("</td>");

            sb.append("<td>");
            sb.append(tr.getAmount().toPlainString());
            sb.append("</td>");

            sb.append("</tr>");
        }
        sb.append("</table>");


        sb.append("<table>");
        sb.append("<th>");
        sb.append("Total Transfer (AION)");
        sb.append("</th>");
        sb.append("<tr>");
        sb.append("<td>");
        sb.append(totalTransfer.toPlainString());
        sb.append("</td>");
        sb.append("</tr>");

        return sb.toString();
    }

}