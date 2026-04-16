package com.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class APIConfig {
    @Value("${demo.api.key}")
    String apiKey;
    @Value("${demo.api.base-url}")
    String baseUrl;
    @Value("${demo.api.model-name}")
    String modelName;
    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return modelName;
    }
}
