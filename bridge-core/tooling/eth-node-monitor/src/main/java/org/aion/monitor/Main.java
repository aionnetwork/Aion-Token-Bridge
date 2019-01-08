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

package org.aion.monitor;

import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.datastore.DbConnectionManager;
import org.aion.monitor.api.EthApi;
import org.aion.monitor.api.EthHttpApi;
import org.aion.monitor.api.EthJsonRpcApi;
import org.aion.monitor.datastore.DatabaseOperations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

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

        DatabaseOperations dbOperation = null;
        DbConnectionManager connectionManager = null;
        try {
            connectionManager = new DbConnectionManager.Builder()
                    .setHost(config.getSql().getHost())
                    .setPort(config.getSql().getPort())
                    .setDatabase(config.getSql().getDbName())
                    .setUser(config.getSql().getUser())
                    .setPassword(config.getSql().getPassword())
                    .build();

            dbOperation = new DatabaseOperations(connectionManager);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Unable to find database driver");
            System.exit(0);
        }

        List<EthApi> ethJsonRpcApis = new ArrayList<>();
        EthApi infuraApi = null;
        EthApi etherscanApi = null;


        for (Config.Client c : config.getEth().getClients()) {
            switch (c.getType()) {
                case "node":
                    Integer id = 0;
                    try {
                        id = dbOperation.insertEthNode(c.getUrl());
                    } catch (PersistenceServiceException e) {
                        System.out.println("Database insert operation failed.");
                        System.exit(0);
                    }
                    ethJsonRpcApis.add(new EthJsonRpcApi(c.getUrl(), c.getName(), id));
                    break;
                case "infura":
                    infuraApi = new EthJsonRpcApi(c.getUrl(), c.getName(), 0);
                    break;
                case "etherscan":
                    etherscanApi = new EthHttpApi(c.getUrl(), c.getName(), config.getEth().getApiKey(), 10);
                    break;
            }
        }

        if (infuraApi == null || ethJsonRpcApis.size() < 1) {
            System.out.println("need at least 1 api connection and 1 other eth connection.");
            System.exit(0);
        }

        BlockRangeValidator validator = new BlockRangeValidator(ethJsonRpcApis,
                infuraApi,
                etherscanApi,
                config.getEth().getMaxAcceptedBlockRange(),
                config.getEth().getMaxAcceptedUnresponsiveCount(),
                config.getEth().getMaxAcceptedSidechainLength(),
                config.getEth().getPollIntervalSeconds(),
                dbOperation);
        validator.start();


    }
}

