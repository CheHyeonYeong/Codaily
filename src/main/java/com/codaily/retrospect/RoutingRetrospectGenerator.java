package com.codaily.retrospect;

import com.codaily.config.CodailyProperties;
import com.codaily.github.RepositoryActivity;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Primary
@Component
public class RoutingRetrospectGenerator implements RetrospectGenerator {

    private final TemplateRetrospectGenerator templateRetrospectGenerator;
    private final GeminiRetrospectGenerator geminiRetrospectGenerator;
    private final OpenAiRetrospectGenerator openAiRetrospectGenerator;
    private final AnthropicRetrospectGenerator anthropicRetrospectGenerator;
    private final CodailyProperties properties;

    public RoutingRetrospectGenerator(
        TemplateRetrospectGenerator templateRetrospectGenerator,
        GeminiRetrospectGenerator geminiRetrospectGenerator,
        OpenAiRetrospectGenerator openAiRetrospectGenerator,
        AnthropicRetrospectGenerator anthropicRetrospectGenerator,
        CodailyProperties properties
    ) {
        this.templateRetrospectGenerator = templateRetrospectGenerator;
        this.geminiRetrospectGenerator = geminiRetrospectGenerator;
        this.openAiRetrospectGenerator = openAiRetrospectGenerator;
        this.anthropicRetrospectGenerator = anthropicRetrospectGenerator;
        this.properties = properties;
    }

    @Override
    public String generate(RepositoryActivity activity, RetrospectGenerationOptions options) {
        AiProvider provider = resolveProvider(options);
        if (provider == AiProvider.TEMPLATE) {
            return templateRetrospectGenerator.generate(activity, options);
        }

        return switch (provider) {
            case GEMINI -> geminiRetrospectGenerator.generate(activity, resolveOptionsForGemini(options));
            case OPENAI -> openAiRetrospectGenerator.generate(activity, resolveOptionsForOpenAi(options));
            case ANTHROPIC -> anthropicRetrospectGenerator.generate(activity, resolveOptionsForAnthropic(options));
            case TEMPLATE -> templateRetrospectGenerator.generate(activity, options);
        };
    }

    private AiProvider resolveProvider(RetrospectGenerationOptions options) {
        AiProvider requestProvider = parseProvider(options.aiProvider());
        if (requestProvider != null) {
            return requestProvider;
        }

        if (StringUtils.hasText(options.geminiApiKey())) {
            return AiProvider.GEMINI;
        }
        if (StringUtils.hasText(options.openAiApiKey())) {
            return AiProvider.OPENAI;
        }
        if (StringUtils.hasText(options.anthropicApiKey())) {
            return AiProvider.ANTHROPIC;
        }

        if (properties.getAi().isEnabled()) {
            AiProvider configured = parseProvider(properties.getAi().getProvider());
            if (configured == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configured AI provider is invalid.");
            }
            return configured;
        }

        return AiProvider.TEMPLATE;
    }

    private AiProvider parseProvider(String rawProvider) {
        try {
            return AiProvider.from(rawProvider);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private RetrospectGenerationOptions resolveOptionsForGemini(RetrospectGenerationOptions options) {
        String apiKey = firstNonBlank(options.geminiApiKey(), properties.getAi().getGeminiApiKey());
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Gemini requires X-Gemini-Api-Key or GEMINI_API_KEY."
            );
        }

        return new RetrospectGenerationOptions(
            options.focus(),
            AiProvider.GEMINI.name().toLowerCase(),
            options.resolvedAiModel(properties.getAi().getModel()),
            apiKey,
            options.openAiApiKey(),
            options.anthropicApiKey()
        );
    }

    private RetrospectGenerationOptions resolveOptionsForOpenAi(RetrospectGenerationOptions options) {
        String apiKey = firstNonBlank(options.openAiApiKey(), properties.getAi().getOpenAiApiKey());
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "OpenAI requires X-OpenAI-Api-Key or OPENAI_API_KEY."
            );
        }

        return new RetrospectGenerationOptions(
            options.focus(),
            AiProvider.OPENAI.name().toLowerCase(),
            options.resolvedAiModel("chat-latest"),
            options.geminiApiKey(),
            apiKey,
            options.anthropicApiKey()
        );
    }

    private RetrospectGenerationOptions resolveOptionsForAnthropic(RetrospectGenerationOptions options) {
        String apiKey = firstNonBlank(options.anthropicApiKey(), properties.getAi().getAnthropicApiKey());
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Anthropic requires X-Anthropic-Api-Key or ANTHROPIC_API_KEY."
            );
        }

        return new RetrospectGenerationOptions(
            options.focus(),
            AiProvider.ANTHROPIC.name().toLowerCase(),
            options.resolvedAiModel("claude-sonnet-4-20250514"),
            options.geminiApiKey(),
            options.openAiApiKey(),
            apiKey
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
