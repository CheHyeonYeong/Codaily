package com.codaily.retrospect;

import com.codaily.config.CodailyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GeminiRetrospectGenerator implements RetrospectGenerator {

    private final RestClient restClient;
    private final RetrospectPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public GeminiRetrospectGenerator(
        RestClient.Builder builder,
        CodailyProperties properties,
        RetrospectPromptBuilder promptBuilder,
        ObjectMapper objectMapper
    ) {
        this.restClient = builder.baseUrl(properties.getAi().getGeminiApiBaseUrl()).build();
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generate(com.codaily.github.RepositoryActivity activity, RetrospectGenerationOptions options) {
        if (!StringUtils.hasText(options.geminiApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini API key is required.");
        }

        String prompt = promptBuilder.buildInstructions() + "\n\n" + promptBuilder.buildInput(activity, options.focus());
        String model = options.resolvedAiModel("gemini-3.5-flash");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        body.put("generationConfig", Map.of(
            "temperature", 0.4,
            "maxOutputTokens", 2048
        ));

        try {
            JsonNode response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("x-goog-api-key", options.geminiApiKey())
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            String text = extractGeminiText(response);
            if (!StringUtils.hasText(text)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned no text output.");
            }
            return text.trim();
        } catch (RestClientResponseException exception) {
            throw toResponseStatusException(exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini API request failed.", exception);
        }
    }

    private String extractGeminiText(JsonNode response) {
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            String partText = part.path("text").asText("");
            if (StringUtils.hasText(partText)) {
                if (text.length() > 0) {
                    text.append("\n");
                }
                text.append(partText.trim());
            }
        }
        return text.toString();
    }

    private ResponseStatusException toResponseStatusException(RestClientResponseException exception) {
        HttpStatus status = switch (exception.getStatusCode().value()) {
            case 400, 401, 403 -> HttpStatus.BAD_REQUEST;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_GATEWAY;
        };

        return new ResponseStatusException(status, extractErrorMessage(exception.getResponseBodyAsString()), exception);
    }

    private String extractErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "Gemini API request failed.";
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            return StringUtils.hasText(message) ? "Gemini API error: " + message : "Gemini API request failed.";
        } catch (JsonProcessingException ignored) {
            return "Gemini API request failed.";
        }
    }
}
