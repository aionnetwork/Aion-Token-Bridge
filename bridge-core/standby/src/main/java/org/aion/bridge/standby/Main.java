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

package org.aion.bridge.standby;

import org.aion.bridge.chain.aion.api.AionJsonRpcConnection;
import org.aion.bridge.chain.aion.api.AionJsonRpcConsolidator;
import org.aion.bridge.chain.aion.types.*;
import org.aion.bridge.chain.base.BlockNumberCollector;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.oracle.ChainOracle;
import org.aion.bridge.chain.base.oracle.ChainOracleBuilder;
import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.AionUnbundlingPolicy;
import org.aion.bridge.chain.bridge.EthBundlingPolicy;
import org.aion.bridge.chain.eth.api.EthJsonRpcConnection;
import org.aion.bridge.chain.eth.api.EthJsonRpcConsolidator;
import org.aion.bridge.chain.eth.types.*;
import org.aion.bridge.chain.log.LoggingSetup;
import org.aion.bridge.datastore.*;
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

    private static DbConnectionManager cm;
    private static ChainOracle<EthBlock, EthReceipt, EthLog, EthAddress> ethChainOracle;
    private static ChainOracle<AionBlock, AionReceipt, AionLog, AionAddress> aionChainOracle;
    private static AionBundleFinalizer aionBundleFinalizer;
    private static AionTipState aionTipState;
    private static BridgeBalanceState balanceState;
    private static AionJsonRpcConsolidator aionConsolidator;
    private static EthJsonRpcConsolidator ethConsolidator;
    private static ThreadPoolExecutor executor;

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

        DataStore ds;
        try {
            Config.Sql sqlConfig = config.getSql();

            cm = new DbConnectionManager.Builder()
                    .setHost(sqlConfig.getHost())
                    .setPort(sqlConfig.getPort())
                    .setDatabase(sqlConfig.getDbName())
                    .setUser(sqlConfig.getUser())
                    .setPassword(sqlConfig.getPassword())
                    .build();

            ds  = new MySqlDatastore(cm);

            Config.Eth ethConfig = config.getEth();
            EthBundlingPolicy ethBundlingPolicy = new EthBundlingPolicy(
                    ethConfig.getTriggerEvent(),
                    ethConfig.getContractAddress());

            Config.Aion aionConfig = config.getAion();
            AionUnbundlingPolicy aionUnbundlingPolicy = new AionUnbundlingPolicy(
                    aionConfig.getContractAddress(),
                    aionConfig.getProcessedEvent(),
                    aionConfig.getDistributedEvent());

            ChainLink ethHistoryStart = new ChainLink(config.getEth().getStartBlock(), new Word32(config.getEth().getStartHash()));
            ChainLink aionHistoryBlock = new ChainLink(config.getAion().getStartBlock(), new Word32(config.getAion().getStartHash()));

            EthChainHistory ethChainHistory = new EthChainHistory(ds, ethHistoryStart, ethBundlingPolicy);
            AionChainHistory aionChainHistory = new AionChainHistory(ds, aionHistoryBlock, aionUnbundlingPolicy);

            List<StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress>> aionConnections = new ArrayList<>();
            for (Config.Client c : config.getAion().getClients()) {
                aionConnections.add(new AionJsonRpcConnection(c.getUrl(), config.getAion().getHttpTimeoutSeconds()));
            }

            List<StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress>> ethConnections = new ArrayList<>();
            for (Config.Client c : config.getEth().getClients()) {
                ethConnections.add(new EthJsonRpcConnection(c.getUrl(), config.getEth().getHttpTimeoutSeconds()));
            }

            int coreThreadCount = 2 * ethConnections.size() + 4 * aionConnections.size();
            executor = new ThreadPoolExecutor(coreThreadCount,
                    3 * coreThreadCount,
                    300,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());

            // Consolidators
            aionConsolidator = new AionJsonRpcConsolidator(
                    aionConnections,
                    config.getAion().getConsolidatorQuorumSize(),
                    Duration.ofSeconds(config.getAion().getConsolidatorTimeoutSeconds()),
                    executor);

            ethConsolidator = new EthJsonRpcConsolidator(
                    ethConnections,
                    config.getEth().getConsolidatorQuorumSize(),
                    Duration.ofSeconds(config.getEth().getConsolidatorTimeoutSeconds()),
                    executor);

            // Block collector for maintaining current tip
            BlockNumberCollector<AionBlock, AionReceipt, AionLog, AionAddress> aionBlockNumberCollector = new BlockNumberCollector<>(
                    aionConnections,
                    config.getAion().getBlockCollectorQuorumSize(),
                    Duration.ofSeconds(config.getAion().getCollectorTimeoutSeconds()),
                    executor);

            BlockNumberCollector<EthBlock, EthReceipt, EthLog, EthAddress> ethBlockNumberCollector = new BlockNumberCollector<>(
                    ethConnections,
                    config.getEth().getBlockCollectorQuorumSize(),
                    Duration.ofSeconds(config.getEth().getCollectorTimeoutSeconds()),
                    executor);

            // Oracles
            ethChainOracle = new ChainOracleBuilder<EthBlock, EthReceipt, EthLog, EthAddress>()
                    .setTipDistance(config.getEth().getFinalityBlocks())
                    .setFilter(new EthEventFilter(
                            new EthAddress(config.getEth().getContractAddress()),
                            config.getEth().getTriggerEvent(),
                            new KeccakBloom()))
                    .setConnection(ethConsolidator.getApi())
                    .setHistory(ethChainHistory)
                    .setBlockCollector(ethBlockNumberCollector)
                    .setBlockBatchSize(config.getEth().getOracleBlockBatchSize())
                    .setReceiptBatchSize(config.getEth().getOracleReceiptBatchSize())
                    .build();

            // Need the tip state to populate the aionLatestBlock table
            aionTipState = new AionTipState.Builder()
                    .setPollInterval(10L, TimeUnit.SECONDS)
                    .setApi(aionBlockNumberCollector)
                    .setDatabase(ds)
                    .setShutdownAfterTipStateError(config.getAion().getShutdownAfterTipStateError())
                    .build();

            balanceState = new BridgeBalanceState.Builder()
                    .setPollInterval(1, TimeUnit.MINUTES)
                    .setApi(new AionJsonRpcConsolidator(aionConnections, config.getAion().getConsolidatorQuorumSize(), executor).getApi())
                    .setBridgeAddress(new AionAddress(aionConfig.getContractAddress()))
                    .setRelayerAddress(new AionAddress(aionConfig.getRelayerAddress()))
                    .setTipState(aionTipState)
                    .setDatabase(ds)
                    .build();

            if (config.getAion().isChainOracleHistoryLoaderSelected()) {
                aionChainOracle = new ChainOracleBuilder<AionBlock, AionReceipt, AionLog, AionAddress>()
                        .setTipDistance(config.getAion().getFinalityBlocks())
                        .setFilter(new AionEventFilter(
                                new AionAddress(config.getAion().getContractAddress()),
                                config.getAion().getProcessedEvent(),
                                new BlakeBloom()))
                        .setConnection(aionConsolidator.getApi())
                        .setHistory(aionChainHistory)
                        .setBlockCollector(aionBlockNumberCollector)
                        .setBlockBatchSize(config.getAion().getOracleBlockBatchSize())
                        .setReceiptBatchSize(config.getAion().getOracleReceiptBatchSize())
                        .build();
            } else {
                aionBundleFinalizer = new AionBundleFinalizer.Builder()
                        .setApi(aionConsolidator.getApi())
                        .setContractAddress(new AionAddress(config.getAion().getContractAddress()))
                        .setDatabase(ds)
                        .setTipState(aionTipState)
                        .setFinalityDepth(config.getAion().getFinalityBlocks())
                        .setHaltDelaySeconds(10)
                        .build();
            }

            // Start the processes

            aionTipState.setName("TAionTipState");
            aionTipState.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            aionTipState.start();

            balanceState.setName("TBalanceState");
            balanceState.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            balanceState.start();

            ethChainOracle.setName("TEthOracle");
            ethChainOracle.setUncaughtExceptionHandler(new CriticalExceptionHandler());
            ethChainOracle.start();

            if (config.getAion().isChainOracleHistoryLoaderSelected()) {
                aionChainOracle.setName("TAionOracle");
                aionChainOracle.setUncaughtExceptionHandler(new CriticalExceptionHandler());
                aionChainOracle.start();
            } else {
                aionBundleFinalizer.setName("TAionFinalizer");
                aionBundleFinalizer.setUncaughtExceptionHandler(new CriticalExceptionHandler());
                aionBundleFinalizer.start();
            }

            Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

        } catch (ClassNotFoundException | SQLException e) {
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
        if (ethChainOracle != null) ethChainOracle.shutdown();
        if (aionChainOracle != null) aionChainOracle.shutdown();
        if (aionBundleFinalizer != null) aionBundleFinalizer.shutdown();
        if (aionTipState != null) aionTipState.shutdown();
        if (balanceState != null) balanceState.shutdown();

        try {
            if (ethChainOracle != null) ethChainOracle.join(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("ethChainOracle shutdown.");

        try {
            if (aionChainOracle != null) aionChainOracle.join(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("aionChainOracle shutdown.");

        try {
            if (aionBundleFinalizer != null) aionBundleFinalizer.join(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("aionBundleFinalizer shutdown.");

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
            cm.closeAllConnections();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        log.info("DbConnectionManager shutdown.");

        aionConsolidator.getApi().evictConnections();
        ethConsolidator.getApi().evictConnections();
        executor.shutdown();
    }
}
