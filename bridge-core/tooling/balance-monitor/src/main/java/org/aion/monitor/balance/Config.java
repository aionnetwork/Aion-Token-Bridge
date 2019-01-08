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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aion {
        private boolean chainOracleHistoryLoaderSelected;
        private List<Client> clients;
        private String contractAddress;
        private String relayerAddress;
        private long startBlock;
        private String startHash;
        private Long httpTimeoutSeconds;
        private Long consolidatorTimeoutSeconds;
        private Long collectorTimeoutSeconds;
        private Integer consolidatorQuorumSize;
        private Integer blockCollectorQuorumSize;
        private Double contractMinimumBalance;
        private Double relayerMinimumBalance;


        public List<Client> getClients() { return clients; }
        public String getContractAddress() { return contractAddress; }
        public String getRelayerAddress() { return relayerAddress; }
        public long getStartBlock() { return startBlock; }
        public String getStartHash() { return startHash; }
        public Long getHttpTimeoutSeconds() { return httpTimeoutSeconds; }
        public Long getConsolidatorTimeoutSeconds() { return consolidatorTimeoutSeconds; }
        public Long getCollectorTimeoutSeconds() { return collectorTimeoutSeconds; }
        public Integer getConsolidatorQuorumSize() { return consolidatorQuorumSize; }
        public Integer getBlockCollectorQuorumSize() { return blockCollectorQuorumSize; }
        public Double getContractMinimumBalance() { return contractMinimumBalance; }
        public Double getRelayerMinimumBalance() { return relayerMinimumBalance; }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Client {
        private String type;
        private String url;

        public String getType() { return type; }
        public String getUrl() { return url; }
    }

    private Aion aion;
    public Aion getAion() { return aion; }

    public static Config load(String path) throws IOException {
        ObjectMapper jackson = new ObjectMapper();
        return jackson.readValue(new File(path), Config.class);
    }

    // make sure no one tries to instantiate this class manually
    private Config() { }
}

