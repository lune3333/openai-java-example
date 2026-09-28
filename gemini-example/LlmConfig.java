public class LlmConfig {
    private String proxyHost;
    private Integer proxyPort;
    private String apiUrl;
    private String apiKey;
    private String certCaFilePath;
    private String model;

    public LlmConfig() {
    }

    public String getProxyHost() { return proxyHost; }
    public void setProxyHost(String proxyHost) { this.proxyHost = proxyHost; }

    public Integer getProxyPort() { return proxyPort; }
    public void setProxyPort(Integer proxyPort) { this.proxyPort = proxyPort; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getCertCaFilePath() { return certCaFilePath; }
    public void setCertCaFilePath(String certCaFilePath) { this.certCaFilePath = certCaFilePath; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}