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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("WeakerAccess")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Signatory {
        private boolean tlsEnabled;
        private String host;
        private int port;
        private String serverCertChainFilePath;
        private String serverPrivateKeyFilePath;
        private String trustCertCollectionFilePath;

        public boolean isTlsEnabled() { return tlsEnabled; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getServerCertChainFilePath() { return serverCertChainFilePath; }
        public String getServerPrivateKeyFilePath() { return serverPrivateKeyFilePath; }
        public String getTrustCertCollectionFilePath() { return trustCertCollectionFilePath; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Enclave {
        private String url;

        public String getUrl() { return url; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Eth {
        private String url;
        private String contractAddress;
        private String triggerEvent;
        private Long httpTimeoutSeconds;

        public String getUrl() { return url; }
        public String getContractAddress() { return contractAddress; }
        public String getTriggerEvent() { return triggerEvent; }
        public Long getHttpTimeoutSeconds() { return httpTimeoutSeconds; }
    }

    private Signatory signatory;
    private Enclave enclave;
    private Eth eth;

    public Signatory getSignatory() { return signatory; }
    public Enclave getEnclave() { return enclave; }
    public Eth getEth() { return eth; }

    public static Config load(String path) throws IOException {
        ObjectMapper jackson = new ObjectMapper();
        return jackson.readValue(new File(path), Config.class);
    }

    // make sure no one tries to instantiate this class manually
    private Config() { }
}

