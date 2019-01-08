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


import org.aion.bridge.chain.aion.api.AionJsonRpcConnection;
import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.aion.types.AionBlock;
import org.aion.bridge.chain.aion.types.AionLog;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.api.IncompleteApiCallException;
import org.aion.bridge.chain.base.api.MalformedApiResponseException;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.eth.api.EthJsonRpcConnection;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;
import org.aion.bridge.datastore.DataStore;
import org.aion.bridge.datastore.DbConnectionManager;
import org.aion.bridge.datastore.MySqlDatastore;
import org.aion.bridge.transferMetrics.balance.BurnAddressTokenBalance;
import org.aion.bridge.transferMetrics.transfer.TotalCoinsTransferred;
import org.aion.bridge.transferMetrics.transfer.TransferLatencyCalculator;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.activation.*;

public class Main {
    private static Double totalAmount = 262673279.0;

    public static void main(String[] args) {

        BigDecimal TOKEN_TO_COIN_DECIMAL_SHIFT = BigDecimal.TEN.pow(18);

        if (args.length < 2) {
            System.out.println("Missing required args.");
            System.out.println("Usage: java Main <config_file_path> <start_point_path>");
            System.exit(0);
        }
        String location = args[0];
        String startLocation = args[1];
        Config config = null;

        try {
            config = Config.load(location);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to load config file");
            System.exit(0);
        }

        DataStore dataStore = null;
        DbConnectionManager connectionManager = null;
        try {
            connectionManager = new DbConnectionManager.Builder()
                    .setHost(config.getSql().getHost())
                    .setPort(config.getSql().getPort())
                    .setDatabase(config.getSql().getDbName())
                    .setUser(config.getSql().getUser())
                    .setPassword(config.getSql().getPassword())
                    .build();

            dataStore = new MySqlDatastore(connectionManager);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Unable to find database driver");
            System.exit(0);
        }

        StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress> ethConnection = new EthJsonRpcConnection(config.getEth().getClients().get(0).getUrl());
        StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress> aionConnection = new AionJsonRpcConnection(config.getAion().getClients().get(0).getUrl());

        BurnAddressTokenBalance burnAddressTokenBalance = new BurnAddressTokenBalance(ethConnection, new EthAddress(config.getEth().getContractAddress()));

        BigInteger balance = burnAddressTokenBalance.getBurnAddressBalance();
        Double BurnAddressAionBalance = 0.0;
        Double swapPercentage = 0.0;
        BigDecimal coinCount = BigDecimal.ZERO;

        if (!balance.equals(BigInteger.ZERO)) {
            BurnAddressAionBalance = balance.longValue() / ((BigInteger.TEN).pow(8).doubleValue());
            swapPercentage = BurnAddressAionBalance / totalAmount;
            System.out.println("burn address balance " + BurnAddressAionBalance);
            System.out.println("swap percentage " + swapPercentage);
        }

        TotalCoinsTransferred coinsTransferred = new TotalCoinsTransferred(dataStore);
        TransferLatencyCalculator transferLatencyCalculator = new TransferLatencyCalculator(dataStore, ethConnection, aionConnection);
        long startBundle = StartpointManager.loadStart(startLocation);
        transferLatencyCalculator.setNewStartBundleId(startBundle);

        if (startBundle == -1) {
            System.out.println("Unable to load start point");
            System.exit(0);
        }

        try {
            Optional<Long> aionFinalizedBundleId = dataStore.getAionFinalizedBundleId();
            List<TimedTransfer> timedTransfers = new ArrayList<>();

            if (aionFinalizedBundleId.isPresent() && aionFinalizedBundleId.get() >= startBundle) {

                coinCount = coinsTransferred.getTotalTransferValue(0L, aionFinalizedBundleId.get());
                timedTransfers = transferLatencyCalculator.getTransferLatency(startBundle, aionFinalizedBundleId.get());
            }

            List<TableRow> rows = new ArrayList<>();
            BigDecimal totalTransfer = BigDecimal.ZERO;
            BigDecimal transferAmount;
            long sumTransferTimes = 0;
            System.out.format("%-68s%-32s%-32s%-32s%-32s\n", "Tx Hash", "EthTimestamp", "AionTimestamp", "TotalDuration", "Amount");
            for (TimedTransfer t : timedTransfers) {
                long duration = t.getAionSealedTimestamp() * 1000 - t.getEthSealedTimestamp() * 1000;
                sumTransferTimes += duration;

                Calendar c1 = Calendar.getInstance();
                Calendar c2 = Calendar.getInstance();

                c1.setTimeInMillis(t.getEthSealedTimestamp() * 1000);
                c2.setTimeInMillis(t.getAionSealedTimestamp() * 1000);
                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

                transferAmount = new BigDecimal(new BigInteger(t.getTransfer().getAionTransferAmount().payload())).divide(TOKEN_TO_COIN_DECIMAL_SHIFT);

                System.out.format("%-68s%-32s%-32s%-32s%-32s\n", t.getTransfer().getEthTxHash(), formatter.format(c1.getTime()),
                        formatter.format(c2.getTime()),
                        DurationFormatUtils.formatDuration(duration, "dd:HH:mm:ss:SSS"),
                        transferAmount.toPlainString());

                rows.add(new TableRow(t.getTransfer().getEthTxHash(),
                        t.getEthSealedTimestamp(), t.getAionSealedTimestamp(), duration,
                        transferAmount));
                totalTransfer = totalTransfer.add(transferAmount);
            }

            // Build CSV
            CSVFormatter csvFormatter = new CSVFormatter();

            //List<String> headerTransfer = new ArrayList<>();
            String headerTransfer = "Transfers during period";
            csvFormatter.write(headerTransfer, totalTransfer);

            // check for large bundle size
            headerTransfer = "Total Withdraws";
            csvFormatter.write(headerTransfer, coinCount);

            headerTransfer = "Burn Address Balance";
            csvFormatter.write(headerTransfer, BurnAddressAionBalance);

            headerTransfer = "Swap Percentage";
            csvFormatter.write(headerTransfer, swapPercentage);

            headerTransfer = "Average Transfer Duration";
            double avgTransferDuration = 0;
            if (timedTransfers.size() > 0)
                avgTransferDuration = (double)sumTransferTimes / timedTransfers.size();
            csvFormatter.write(headerTransfer, avgTransferDuration);


            List<String> header = new ArrayList<>();
            header.add("Tx Hash");
            header.add("Eth Timestamp");
            header.add("Aion Timestamp");
            header.add("Total Duration");
            header.add("Amount");
            csvFormatter.write(header, rows);

            String filename = csvFormatter.getFilename();

            try {
                Config.Smtp smtp = config.getSmtp();

                String from = smtp.getFrom();

                Properties properties = System.getProperties();
                properties.setProperty("mail.smtp.host", smtp.getHost());
                properties.put("mail.smtp.port", smtp.getPort());
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.socketFactory.class",
                        "javax.net.ssl.SSLSocketFactory");
                properties.setProperty("mail.user", smtp.getUser());
                properties.setProperty("mail.password", smtp.getPassword());

                Session session = Session.getDefaultInstance(properties,
                        new javax.mail.Authenticator() {

                            // override the getPasswordAuthentication
                            // method
                            protected PasswordAuthentication
                            getPasswordAuthentication() {
                                return new PasswordAuthentication(smtp.getUser(),
                                        smtp.getPassword());
                            }
                        });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));

                for (String to : smtp.getTo()) {
                    message.addRecipient(Message.RecipientType.TO,
                            new InternetAddress(to));
                }

                message.setSubject("Bridge Transfer Report");

                // Create a multipar message
                Multipart multipart = new MimeMultipart();

                // Create the message part
                BodyPart messageBodyPart;

                if (filename != null) {
                    // Add the attachment
                    messageBodyPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(filename);
                    messageBodyPart.setDataHandler(new DataHandler(source));
                    messageBodyPart.setFileName(filename);
                    multipart.addBodyPart(messageBodyPart);
                }

                message.setContent(multipart);
                //message.setContent(TableFormater.formatTable("TxHash EthTimestamp AionTimestamp TotalDuration Amount", rows, totalTransfer), "text/html");

                Transport.send(message);

                StartpointManager.updateStart(transferLatencyCalculator.getNewStartBundleId(), startLocation);
            } catch (AddressException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            }

            connectionManager.closeAllConnections();

        } catch (QuorumNotAvailableException e) {
            e.printStackTrace();
        } catch (MalformedApiResponseException e) {
            e.printStackTrace();
        } catch (PersistenceServiceException e) {
            e.printStackTrace();
        } catch (IncompleteApiCallException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}