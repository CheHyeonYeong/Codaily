package com.codaily.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codaily")
public class CodailyProperties {

    private final App app = new App();
    private final Github github = new Github();
    private final Ai ai = new Ai();

    public App getApp() {
        return app;
    }

    public Github getGithub() {
        return github;
    }

    public Ai getAi() {
        return ai;
    }

    public static class App {

        private String publicBaseUrl = "http://localhost:8080";

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public static class Github {

        private boolean demo = true;
        private String apiBaseUrl = "https://api.github.com";
        private String personalAccessToken;

        public boolean isDemo() {
            return demo;
        }

        public void setDemo(boolean demo) {
            this.demo = demo;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getPersonalAccessToken() {
            return personalAccessToken;
        }

        public void setPersonalAccessToken(String personalAccessToken) {
            this.personalAccessToken = personalAccessToken;
        }
    }

    public static class Ai {

        private boolean enabled;
        private String provider = "template";
        private String model = "gemini-3.5-flash";
        private String geminiApiBaseUrl = "https://generativelanguage.googleapis.com";
        private String openAiApiBaseUrl = "https://api.openai.com";
        private String anthropicApiBaseUrl = "https://api.anthropic.com";
        private String openAiApiKey;
        private String anthropicApiKey;
        private String geminiApiKey;
        private String anthropicVersion = "2023-06-01";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

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

        public String getGeminiApiBaseUrl() {
            return geminiApiBaseUrl;
        }

        public void setGeminiApiBaseUrl(String geminiApiBaseUrl) {
            this.geminiApiBaseUrl = geminiApiBaseUrl;
        }

        public String getOpenAiApiBaseUrl() {
            return openAiApiBaseUrl;
        }

        public void setOpenAiApiBaseUrl(String openAiApiBaseUrl) {
            this.openAiApiBaseUrl = openAiApiBaseUrl;
        }

        public String getAnthropicApiBaseUrl() {
            return anthropicApiBaseUrl;
        }

        public void setAnthropicApiBaseUrl(String anthropicApiBaseUrl) {
            this.anthropicApiBaseUrl = anthropicApiBaseUrl;
        }

        public String getGeminiApiKey() {
            return geminiApiKey;
        }

        public void setGeminiApiKey(String geminiApiKey) {
            this.geminiApiKey = geminiApiKey;
        }

        public String getOpenAiApiKey() {
            return openAiApiKey;
        }

        public void setOpenAiApiKey(String openAiApiKey) {
            this.openAiApiKey = openAiApiKey;
        }

        public String getAnthropicApiKey() {
            return anthropicApiKey;
        }

        public void setAnthropicApiKey(String anthropicApiKey) {
            this.anthropicApiKey = anthropicApiKey;
        }

        public String getAnthropicVersion() {
            return anthropicVersion;
        }

        public void setAnthropicVersion(String anthropicVersion) {
            this.anthropicVersion = anthropicVersion;
        }
    }
}
