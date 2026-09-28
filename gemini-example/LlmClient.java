import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class LlmClient {

    private final OpenAIClient openAIClient;
    private final LlmConfig config;

    // Private constructor enforced so it can only be instantiated via the Builder
    private LlmClient(Builder builder) {
        this.config = builder.config;

        OpenAIOkHttpClient.Builder okHttpClientBuilder = OpenAIOkHttpClient.builder();

        if (config.getApiKey() != null) {
            okHttpClientBuilder.apiKey(config.getApiKey());
        }

        if (config.getApiUrl() != null) {
            okHttpClientBuilder.baseUrl(config.getApiUrl());
        }

        if (config.getProxyHost() != null && config.getProxyPort() != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getProxyHost(), config.getProxyPort()));
            okHttpClientBuilder.proxy(proxy);
        }

        if (config.getCertCaFilePath() != null) {
            try {
                // Load the CA certificate from the provided file path
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate caCert;
                try (FileInputStream fis = new FileInputStream(config.getCertCaFilePath())) {
                    caCert = (X509Certificate) cf.generateCertificate(fis);
                }

                // Put the certificate in an empty Keystore
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("caCert", caCert);

                // Create a TrustManager that trusts the CAs in our KeyStore
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                TrustManager[] trustManagers = tmf.getTrustManagers();
                X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                // Create an SSLContext that uses our TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, null);

                // Inject the SSL Socket Factory into the OkHttp Builder
                okHttpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);

            } catch (Exception e) {
                throw new RuntimeException("Failed to configure SSL with the provided CA certificate: " + config.getCertCaFilePath(), e);
            }
        }

        // Finalize the actual SDK client implementation
        this.openAIClient = okHttpClientBuilder.build();
    }

    /**
     * @return The strictly configured OpenAIClient ready for making requests.
     */
    public OpenAIClient getOpenAIClient() {
        return openAIClient;
    }

    /**
     * @return The underlying LlmConfig, helpful for fetching properties like the 'model'.
     */
    public LlmConfig getConfig() {
        return config;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LlmConfig config = new LlmConfig();

        public Builder apiKey(String apiKey) {
            this.config.setApiKey(apiKey);
            return this;
        }

        public Builder apiUrl(String apiUrl) {
            this.config.setApiUrl(apiUrl);
            return this;
        }

        public Builder proxy(String host, Integer port) {
            this.config.setProxyHost(host);
            this.config.setProxyPort(port);
            return this;
        }

        public Builder certCaFilePath(String path) {
            this.config.setCertCaFilePath(path);
            return this;
        }

        public Builder model(String model) {
            this.config.setModel(model);
            return this;
        }

        public LlmClient build() {
            return new LlmClient(this);
        }
    }
}