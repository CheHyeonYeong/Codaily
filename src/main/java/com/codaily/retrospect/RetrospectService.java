package com.codaily.retrospect;

import com.codaily.config.CodailyProperties;
import com.codaily.github.GithubActivityService;
import com.codaily.github.RepositoryActivity;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RetrospectService {

    private final RetrospectRepository retrospectRepository;
    private final GithubActivityService githubActivityService;
    private final RetrospectGenerator retrospectGenerator;
    private final CodailyProperties properties;
    private final Clock clock;

    public RetrospectService(
        RetrospectRepository retrospectRepository,
        GithubActivityService githubActivityService,
        RetrospectGenerator retrospectGenerator,
        CodailyProperties properties,
        Clock clock
    ) {
        this.retrospectRepository = retrospectRepository;
        this.githubActivityService = githubActivityService;
        this.retrospectGenerator = retrospectGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    @CacheEvict(value = "retrospect-rankings", allEntries = true)
    public RetrospectResponse createRetrospect(
        CreateRetrospectRequest request,
        String geminiApiKey,
        String openAiApiKey,
        String anthropicApiKey,
        Authentication authentication
    ) {
        RepositoryActivity activity = githubActivityService.fetchActivity(
            request.owner(),
            request.repo(),
            request.resolvedCommitLimit(),
            request.resolvedPullRequestLimit(),
            authentication
        );

        String markdown = retrospectGenerator.generate(
            activity,
            new RetrospectGenerationOptions(
                request.focus(),
                request.aiProvider(),
                request.aiModel(),
                geminiApiKey,
                openAiApiKey,
                anthropicApiKey
            )
        );
        LocalDateTime now = LocalDateTime.now(clock);

        RetrospectEntity entity = new RetrospectEntity();
        entity.setRepoFullName(activity.repoFullName());
        entity.setTitle(activity.repoFullName() + " retrospective");
        entity.setMarkdownContent(markdown);
        entity.setViewCount(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        RetrospectEntity saved = retrospectRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "retrospect-rankings", allEntries = true)
    public RetrospectResponse getRetrospect(UUID id) {
        RetrospectEntity entity = retrospectRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."));

        entity.setViewCount(entity.getViewCount() + 1);
        entity.setUpdatedAt(LocalDateTime.now(clock));

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    @Cacheable("retrospect-rankings")
    public List<RetrospectSummaryResponse> getTopRetrospects(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return retrospectRepository.findAllByOrderByViewCountDescCreatedAtDesc(PageRequest.of(0, safeLimit))
            .stream()
            .map(this::toSummary)
            .toList();
    }

    private RetrospectResponse toResponse(RetrospectEntity entity) {
        return new RetrospectResponse(
            entity.getId(),
            entity.getRepoFullName(),
            entity.getTitle(),
            entity.getMarkdownContent(),
            entity.getViewCount(),
            entity.getCreatedAt(),
            properties.getApp().getPublicBaseUrl() + "/retrospects/" + entity.getId()
        );
    }

    private RetrospectSummaryResponse toSummary(RetrospectEntity entity) {
        return new RetrospectSummaryResponse(
            entity.getId(),
            entity.getRepoFullName(),
            entity.getTitle(),
            entity.getViewCount(),
            entity.getCreatedAt(),
            properties.getApp().getPublicBaseUrl() + "/retrospects/" + entity.getId()
        );
    }
}
