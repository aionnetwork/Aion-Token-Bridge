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

package org.aion.bridge.signatory;

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
