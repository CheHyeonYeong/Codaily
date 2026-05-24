package com.codaily.retrospect;

import org.springframework.util.StringUtils;

public record RetrospectGenerationOptions(
    String focus,
    String aiProvider,
    String aiModel,
    String geminiApiKey,
    String openAiApiKey,
    String anthropicApiKey
) {

    public String resolvedAiModel(String defaultModel) {
        return StringUtils.hasText(aiModel) ? aiModel : defaultModel;
    }
}
