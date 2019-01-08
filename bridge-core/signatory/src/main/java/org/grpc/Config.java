package org.grpc;

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

