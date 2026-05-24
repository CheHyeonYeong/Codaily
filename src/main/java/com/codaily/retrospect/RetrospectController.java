package com.codaily.retrospect;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/retrospects")
public class RetrospectController {

    private final RetrospectService retrospectService;

    public RetrospectController(RetrospectService retrospectService) {
        this.retrospectService = retrospectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RetrospectResponse create(
        @RequestHeader(value = "X-Gemini-Api-Key", required = false) String geminiApiKey,
        @RequestHeader(value = "X-OpenAI-Api-Key", required = false) String openAiApiKey,
        @RequestHeader(value = "X-Anthropic-Api-Key", required = false) String anthropicApiKey,
        @Valid @RequestBody CreateRetrospectRequest request,
        Authentication authentication
    ) {
        return retrospectService.createRetrospect(request, geminiApiKey, openAiApiKey, anthropicApiKey, authentication);
    }

    @GetMapping("/{id}")
    public RetrospectResponse get(@PathVariable UUID id) {
        return retrospectService.getRetrospect(id);
    }
}
