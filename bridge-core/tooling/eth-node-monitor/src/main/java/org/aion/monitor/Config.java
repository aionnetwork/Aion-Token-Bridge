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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Config {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Eth {
        private List<Client> clients;
        private Long pollIntervalSeconds;
        private Integer maxAcceptedBlockRange;
        private Integer maxAcceptedUnresponsiveCount;
        private Integer maxAcceptedSidechainLength;
        private String apiKey;

        public List<Client> getClients() { return clients; }
        public long getPollIntervalSeconds() { return pollIntervalSeconds; }
        public Integer getMaxAcceptedBlockRange() { return maxAcceptedBlockRange; }
        public Integer getMaxAcceptedUnresponsiveCount() { return maxAcceptedUnresponsiveCount; }
        public Integer getMaxAcceptedSidechainLength() { return maxAcceptedSidechainLength; }
        public String getApiKey() { return apiKey; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sql {
        private String user;
        private String password;
        private String dbName;
        private String host;
        private String port;

        public String getUser() { return user; }
        public String getPassword() { return password; }
        public String getDbName() { return dbName; }
        public String getHost() { return host; }
        public String getPort() { return port; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Client {
        private String type;
        private String url;
        private String name;

        public String getType() { return type; }
        public String getUrl() { return url; }
        public String getName() { return name; }
    }

    private Eth eth;
    private Sql sql;

    public Eth getEth() { return eth; }
    public Sql getSql() { return sql; }


    public static Config load(String path) throws IOException {
        ObjectMapper jackson = new ObjectMapper();
        return jackson.readValue(new File(path), Config.class);
    }

    // make sure no one tries to instantiate this class manually
    private Config() { }
}
