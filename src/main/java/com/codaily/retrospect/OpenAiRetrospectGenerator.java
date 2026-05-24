package com.codaily.retrospect;

import com.codaily.config.CodailyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
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
public class OpenAiRetrospectGenerator implements RetrospectGenerator {

    private final RestClient restClient;
    private final RetrospectPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public OpenAiRetrospectGenerator(
        RestClient.Builder builder,
        CodailyProperties properties,
        RetrospectPromptBuilder promptBuilder,
        ObjectMapper objectMapper
    ) {
        this.restClient = builder.baseUrl(properties.getAi().getOpenAiApiBaseUrl()).build();
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generate(com.codaily.github.RepositoryActivity activity, RetrospectGenerationOptions options) {
        if (!StringUtils.hasText(options.openAiApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenAI API key is required.");
        }

        String model = options.resolvedAiModel("chat-latest");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", promptBuilder.buildInstructions());
        body.put("input", promptBuilder.buildInput(activity, options.focus()));
        body.put("max_output_tokens", 2048);
        body.put("temperature", 0.4);

        try {
            JsonNode response = restClient.post()
                .uri("/v1/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + options.openAiApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            String text = extractOpenAiText(response);
            if (!StringUtils.hasText(text)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI returned no text output.");
            }
            return text.trim();
        } catch (RestClientResponseException exception) {
            throw toResponseStatusException(exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI API request failed.", exception);
        }
    }

    private String extractOpenAiText(JsonNode response) {
        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }

            for (JsonNode part : content) {
                String type = part.path("type").asText("");
                String partText = part.path("text").asText("");
                if (("output_text".equals(type) || "text".equals(type)) && StringUtils.hasText(partText)) {
                    if (text.length() > 0) {
                        text.append("\n");
                    }
                    text.append(partText.trim());
                }
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
            return "OpenAI API request failed.";
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            return StringUtils.hasText(message) ? "OpenAI API error: " + message : "OpenAI API request failed.";
        } catch (JsonProcessingException ignored) {
            return "OpenAI API request failed.";
        }
    }
}
