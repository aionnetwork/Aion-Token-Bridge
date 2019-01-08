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

package org.aion.monitor.balance;

import org.aion.bridge.chain.aion.api.AionJsonRpcConnection;
import org.aion.bridge.chain.aion.api.AionJsonRpcConsolidator;
import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.aion.types.AionBlock;
import org.aion.bridge.chain.aion.types.AionLog;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.BlockNumberCollector;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.log.LoggingSetup;
import org.aion.bridge.datastore.AionTipState;
import org.aion.bridge.datastore.BridgeBalanceState;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Duplicates")
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static BridgeBalanceState balanceState;
    private static AionTipState aionTipState;
    private static BalanceValidator contractBalanceValidator;
    private static BalanceValidator relayerBalanceValidator;

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Missing the config file location.");
            System.exit(0);
        }
        String location = args[0];
        Config config = null;

        try {
            config = Config.load(location);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to load config file");
            System.exit(0);
        }

        Config.Aion aionConfig = config.getAion();

        List<StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress>> aionConnections = new ArrayList<>();
        for (Config.Client c : aionConfig.getClients()) {
            aionConnections.add(new AionJsonRpcConnection(c.getUrl(), aionConfig.getHttpTimeoutSeconds()));
        }

        int coreThreadCount = aionConnections.size();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreadCount,
                3 * coreThreadCount,
                300,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        try {
            BlockNumberCollector<AionBlock, AionReceipt, AionLog, AionAddress> aionBlockNumberCollector = new BlockNumberCollector<>(
                    aionConnections,
                    aionConfig.getBlockCollectorQuorumSize(),
                    Duration.ofSeconds(aionConfig.getCollectorTimeoutSeconds()),
                    executor);

            aionTipState = new AionTipState.Builder()
                    .setPollInterval(10L, TimeUnit.SECONDS)
                    .setApi(aionBlockNumberCollector)
                    .setDatabase(null)
                    .build();

            balanceState = new BridgeBalanceState.Builder()
                    .setPollInterval(30, TimeUnit.SECONDS)
                    .setApi(new AionJsonRpcConsolidator(aionConnections, aionConfig.getConsolidatorQuorumSize(), Duration.ofSeconds(config.getAion().getConsolidatorTimeoutSeconds()),executor).getApi())
                    .setBridgeAddress(new AionAddress(aionConfig.getContractAddress()))
                    .setRelayerAddress(new AionAddress(aionConfig.getRelayerAddress()))
                    .setTipState(aionTipState)
                    .setDatabase(null)
                    .build();

            contractBalanceValidator = new BalanceValidator(balanceState,
                    new AionAddress(aionConfig.getContractAddress()),
                    aionConfig.getContractMinimumBalance(),
                    40,
                    TimeUnit.SECONDS);

            relayerBalanceValidator = new BalanceValidator(balanceState,
                    new AionAddress(aionConfig.getRelayerAddress()),
                    aionConfig.getRelayerMinimumBalance(),
                    40,
                    TimeUnit.SECONDS);

            aionTipState.setName("TAionTipState");
            aionTipState.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            aionTipState.start();

            balanceState.setName("TBalanceState");
            balanceState.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            balanceState.start();

            contractBalanceValidator.start();
            relayerBalanceValidator.start();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class CriticalExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LoggingSetup.setupLogging();
            log.error(LoggingSetup.SMTP_MARKER, "Caught a critical error from thread: {}\n Trace: {}", t.getName(), ExceptionUtils.getStackTrace(e));
            shutdown();
        }
    }

    private static void shutdown() {
        if (aionTipState != null) aionTipState.shutdown();
        if (balanceState != null) balanceState.shutdown();
        if (contractBalanceValidator != null) contractBalanceValidator.shutdown();
        if (relayerBalanceValidator != null) relayerBalanceValidator.shutdown();

        try {
            if (aionTipState != null) aionTipState.join(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("aionTipState shutdown.");

        try {
            if (balanceState != null) balanceState.join(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("balanceState shutdown.");
        try {
            if (contractBalanceValidator != null) contractBalanceValidator.join(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("contractBalanceValidator shutdown.");
        try {
            if (relayerBalanceValidator != null) relayerBalanceValidator.join(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("relayerBalanceValidator shutdown.");

    }
}
