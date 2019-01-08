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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

public class CSVFormatter {
    private String filename;

    public CSVFormatter() {
        // Create reports directory if needed
        File directory = new File(".//report");
        if(!directory.exists())
            directory.mkdir();

        LocalDateTime now = LocalDateTime.now();
        filename = ".//report//bridge_report_"+now.getDayOfMonth()+"_"+now.getMonthValue()+"_"+now.getYear()+".csv";
    }

    public String getFilename() {
        return filename;
    }

    public void write(String header, BigDecimal value) {
        try (
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header))
        ) {
            csvPrinter.printRecord(value);
            csvPrinter.printRecord("");

            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String header, Double value) {
        try (
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header))
        ) {
            csvPrinter.printRecord(value.toString());
            csvPrinter.printRecord("");

            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(List<String> header, List<TableRow> rows) {
        try (
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header.toArray(new String[header.size()])))
        ) {

            for(TableRow r : rows) {
                csvPrinter.printRecord(r.toTableRow());
            }

            csvPrinter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}