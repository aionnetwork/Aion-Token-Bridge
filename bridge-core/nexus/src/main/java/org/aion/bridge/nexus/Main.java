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

package org.aion.bridge.nexus;

import org.aion.bridge.chain.aion.api.AionJsonRpcConnection;
import org.aion.bridge.chain.aion.api.AionJsonRpcConsolidator;
import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.aion.types.AionBlock;
import org.aion.bridge.chain.aion.types.AionLog;
import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.BlockNumberCollector;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.oracle.ChainOracle;
import org.aion.bridge.chain.base.oracle.ChainOracleBuilder;
import org.aion.bridge.chain.base.types.ChainLink;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.AionUnbundlingPolicy;
import org.aion.bridge.chain.bridge.EthBundlingPolicy;
import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.chain.eth.api.EthJsonRpcConnection;
import org.aion.bridge.chain.eth.api.EthJsonRpcConsolidator;
import org.aion.bridge.chain.eth.types.*;
import org.aion.bridge.datastore.AionTipState;
import org.aion.bridge.datastore.DataStore;
import org.aion.bridge.datastore.DbConnectionManager;
import org.aion.bridge.datastore.MySqlDatastore;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    // Initialize all threads and processes
    public static String location;
    private static final boolean persist = true;

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Missing the config file location.");
            System.exit(0);
        }
        location = args[0];
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
        if(persist) {
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
        }

        EthBundlingPolicy ethBundlingPolicy =
                new EthBundlingPolicy(config.getEth().getTriggerEvent(), config.getEth().getContractAddress());

        List<StatelessChainConnection<AionBlock, AionReceipt, AionLog, AionAddress>> aionConnections = new ArrayList<>();
        for (Config.Client c : config.getAion().getClients()) {
            aionConnections.add(new AionJsonRpcConnection(c.getUrl(), config.getAion().getHttpTimeoutSeconds()));
        }

        List<StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress>> ethConnections = new ArrayList<>();
        for (Config.Client c : config.getEth().getClients()) {
            ethConnections.add(new EthJsonRpcConnection(c.getUrl(), config.getEth().getHttpTimeoutSeconds()));
        }

        // Build list of signatory connections
        List<SignatoryGrpcConnection> signatoryGrpcConnections = new ArrayList<>();
        for (Config.Signatory signatory : config.getSignatories()) {
            try {
                SignatoryGrpcConnection connection = new SignatoryGrpcConnection.Builder()
                        .publicKey(signatory.getPublicKey())
                        .host(signatory.getUrl().split(":")[0])
                        .port(Integer.parseInt(signatory.getUrl().split(":")[1]))
                        .tlsEnabled(signatory.isTlsEnabled())
                        .clientCertChainFilePath(signatory.getClientCertChainFilePath())
                        .clientPrivateKeyFilePath(signatory.getClientPrivateKeyFilePath())
                        .trustCertCollectionFilePath(signatory.getTrustCertCollectionFilePath())
                        .build();

                signatoryGrpcConnections.add(connection);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // EthOracle, EthBlockCollector | AionClient, CollectReceipt, FinalizeBundle, AionBlockCollector | SignatureCollector
        int coreThreadCount = 2 * ethConnections.size() + 4 * aionConnections.size() + signatoryGrpcConnections.size();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreadCount,
                3 * coreThreadCount,
                300,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        // Need signature from all signatories
        SignatoryCollector signatoryCollector = new SignatoryCollector(
                signatoryGrpcConnections,
                config.getBridge().getSignatoryQuorumSize(),
                Duration.ofSeconds(config.getBridge().getSignatoryCollectorTimeoutSeconds()),
                executor);

        // Aion Consolidator
        AionJsonRpcConsolidator aionConsolidator = new AionJsonRpcConsolidator(
                aionConnections,
                config.getAion().getConsolidatorQuorumSize(),
                Duration.ofSeconds(config.getAion().getConsolidatorTimeoutSeconds()),
                executor);

        EthJsonRpcConsolidator ethConsolidator = new EthJsonRpcConsolidator(
                ethConnections,
                config.getEth().getConsolidatorQuorumSize(),
                Duration.ofSeconds(config.getEth().getConsolidatorTimeoutSeconds()),
                executor);

        // Relayer
        Relayer relayer = new Relayer(
                new Enclave(config.getRelayer().getEnclaveUrl()),
                new AionAddress(config.getRelayer().getAddress()),
                config.getRelayer().getPublicKey());

        // Initialize nonce for Relayer
        try{
            BigInteger nonce = aionConsolidator.getApi().getNonce(relayer.getAccountAddress());
            relayer.setNonce(nonce);
        } catch (QuorumNotAvailableException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Unable to get nonce for relayer. Shutting down.");
            System.exit(0);
        }

        // Eth start query block
        ChainLink startBlock = new ChainLink(config.getEth().getStartBlock(), new Word32(config.getEth().getStartHash()));

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

        AionUnbundlingPolicy unbundlingPolicy = new AionUnbundlingPolicy(
                config.getAion().getContractAddress(),
                config.getAion().getProcessedEvent(),
                config.getAion().getDistributedEvent());

        Bridge bridge = null;
        try {
            AionTipState tipState = new AionTipState.Builder()
                    .setPollInterval(10L, TimeUnit.SECONDS)
                    .setApi(aionBlockNumberCollector)
                    .setDatabase(dataStore)
                    .setShutdownAfterTipStateError(config.getAion().getShutdownAfterTipStateError())
                    .build();

            AionClient aionClient = new AionClient(aionConsolidator.getApi(), tipState, relayer);

            bridge = new Bridge.Builder()
                    .setDatabase(dataStore, connectionManager)
                    .setAionTipState(tipState)
                    .setSignatoryCollector(signatoryCollector)
                    .setAionClient(aionClient)
                    .setAionJsonRpcConsolidator(aionConsolidator)
                    .setEthJsonRpcConsolidator(ethConsolidator)
                    .setStartBlock(startBlock)
                    .setEthBundlingPolicy(ethBundlingPolicy)
                    .setReceiptMinDepth(config.getAion().getReceiptMinDepth())
                    .setAionFinalizationLimit(config.getAion().getFinalityBlocks())
                    .setUnbundlingPolicy(unbundlingPolicy)
                    .setSuccessfulTxHashEvent(config.getAion().getSuccessfulTxHashEvent())
                    .setExecutor(executor)
                    .build();

            // unchecked-warning suppressed: adding more generic args to ChainOracle would make declaration unfashionably verbose :(
            @SuppressWarnings("unchecked")
            ChainOracle<EthBlock, EthReceipt, EthLog, EthAddress> chainOracle = new ChainOracleBuilder<EthBlock, EthReceipt, EthLog, EthAddress>()
                    .setTipDistance(config.getEth().getFinalityBlocks())
                    .setFilter(new EthEventFilter(
                            new EthAddress(config.getEth().getContractAddress()),
                            config.getEth().getTriggerEvent(),
                            new KeccakBloom()))
                    .setConnection(ethConsolidator.getApi())
                    .setHistory(bridge.getChainHistory())
                    .setBlockCollector(ethBlockNumberCollector)
                    .setBlockBatchSize(config.getEth().getOracleBlockBatchSize())
                    .setReceiptBatchSize(config.getEth().getOracleReceiptBatchSize())
                    .build();

            // Pass to allow bridge to allow bridge to have control over all threads
            bridge.setBridgeOracle(chainOracle);

            // Initialize and start all threads
            bridge.initializeThreads(1,1); // Only 1 thread for each process for now

            // Shutdown hook, add before starting due to blocking nature of
            Runtime.getRuntime().addShutdownHook(
                    new Thread(bridge::shutdown)
            );

            // Start all tasks + chain oracle
            bridge.start();


        } catch (PersistenceServiceException | InterruptedException e) {
            e.printStackTrace();
            bridge.shutdown();
            System.exit(-1);
        }
    }

}
