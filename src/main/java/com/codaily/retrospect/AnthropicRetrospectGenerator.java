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
public class AnthropicRetrospectGenerator implements RetrospectGenerator {

    private final RestClient restClient;
    private final RetrospectPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final CodailyProperties properties;

    public AnthropicRetrospectGenerator(
        RestClient.Builder builder,
        CodailyProperties properties,
        RetrospectPromptBuilder promptBuilder,
        ObjectMapper objectMapper
    ) {
        this.restClient = builder.baseUrl(properties.getAi().getAnthropicApiBaseUrl()).build();
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String generate(com.codaily.github.RepositoryActivity activity, RetrospectGenerationOptions options) {
        if (!StringUtils.hasText(options.anthropicApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anthropic API key is required.");
        }

        String model = options.resolvedAiModel("claude-sonnet-4-20250514");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("system", promptBuilder.buildInstructions());
        body.put("max_tokens", 2048);
        body.put("messages", List.of(Map.of(
            "role", "user",
            "content", promptBuilder.buildInput(activity, options.focus())
        )));

        try {
            JsonNode response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", options.anthropicApiKey())
                .header("anthropic-version", properties.getAi().getAnthropicVersion())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            String text = extractAnthropicText(response);
            if (!StringUtils.hasText(text)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Anthropic returned no text output.");
            }
            return text.trim();
        } catch (RestClientResponseException exception) {
            throw toResponseStatusException(exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Anthropic API request failed.", exception);
        }
    }

    private String extractAnthropicText(JsonNode response) {
        JsonNode content = response.path("content");
        if (!content.isArray()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode part : content) {
            String type = part.path("type").asText("");
            String partText = part.path("text").asText("");
            if (("text".equals(type) || type.isBlank()) && StringUtils.hasText(partText)) {
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
            return "Anthropic API request failed.";
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            return StringUtils.hasText(message) ? "Anthropic API error: " + message : "Anthropic API request failed.";
        } catch (JsonProcessingException ignored) {
            return "Anthropic API request failed.";
        }
    }
}
