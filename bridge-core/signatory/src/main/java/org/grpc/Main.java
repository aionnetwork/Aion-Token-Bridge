package org.grpc;

import org.aion.bridge.chain.bridge.EthBundlingPolicy;
import org.aion.bridge.chain.eth.api.EthJsonRpcConnection;
import org.aion.bridge.chain.log.LoggingSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static String CONFIG_FILE_PATH = "config.json";

    public static void main(String[] args) {
        if(args.length >= 1)
            CONFIG_FILE_PATH = args[0];

        LoggingSetup.setupLogging();
        log.info("Signatory service starting ...");
        log.info("Using config file: {}", CONFIG_FILE_PATH);

        try {
            Config config = Config.load(CONFIG_FILE_PATH);

            Enclave enclave = new Enclave(config.getEnclave().getUrl());

            EthBundlingPolicy ethBundlingPolicy = new EthBundlingPolicy(config.getEth().getTriggerEvent(), config.getEth().getContractAddress());

            EthJsonRpcConnection api = new EthJsonRpcConnection(config.getEth().getUrl(), config.getEth().getHttpTimeoutSeconds());
            BundleValidator<EthJsonRpcConnection> validator = new BundleValidator<>(api, ethBundlingPolicy);

            Controller<EthJsonRpcConnection> controller = new Controller<>(enclave, validator);

            Config.Signatory s = config.getSignatory();
            Server server = new Server.Builder()
                    .tlsEnabled(s.isTlsEnabled())
                    .host(s.getHost())
                    .port(s.getPort())
                    .serverCertChainFilePath(s.getServerCertChainFilePath())
                    .serverPrivateKeyFilePath(s.getServerPrivateKeyFilePath())
                    .trustCertCollectionFilePath(s.getTrustCertCollectionFilePath())
                    .controller(controller)
                    .build();

            server.start();
            server.runUntilShutdown();
        } catch (Exception e) {
            LoggingSetup.setupLogging();
            log.error("Signatory server shutting down with top-level exception:", e);
        }
    }
}
