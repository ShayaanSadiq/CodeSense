package com.codesense.prototype.web.dto;

import jakarta.validation.constraints.Size;

public class LlmConfigRequest {

    @Size(max = 32)
    private String provider;
    @Size(max = 80)
    private String model;
    @Size(max = 256)
    private String apiKey;
    @Size(max = 200)
    private String baseUrl;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
