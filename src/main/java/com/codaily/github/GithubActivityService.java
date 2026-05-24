package com.codaily.github;

import com.codaily.config.CodailyProperties;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GithubActivityService {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
        new ParameterizedTypeReference<>() {
        };

    private final RestClient githubRestClient;
    private final GithubTokenResolver tokenResolver;
    private final CodailyProperties properties;

    public GithubActivityService(
        RestClient githubRestClient,
        GithubTokenResolver tokenResolver,
        CodailyProperties properties
    ) {
        this.githubRestClient = githubRestClient;
        this.tokenResolver = tokenResolver;
        this.properties = properties;
    }

    public RepositoryActivity fetchActivity(
        String owner,
        String repo,
        int commitLimit,
        int pullRequestLimit,
        Authentication authentication
    ) {
        int safeCommitLimit = normalizeLimit(commitLimit);
        int safePullRequestLimit = normalizeLimit(pullRequestLimit);

        if (properties.getGithub().isDemo()) {
            return demoActivity(owner, repo, safeCommitLimit, safePullRequestLimit);
        }

        String token = tokenResolver.resolve(authentication).orElseThrow(() ->
            new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "GitHub OAuth 또는 GITHUB_PERSONAL_ACCESS_TOKEN 설정이 필요합니다."
            )
        );

        try {
            List<Map<String, Object>> commits = githubRestClient.get()
                .uri("/repos/{owner}/{repo}/commits?per_page={limit}", owner, repo, safeCommitLimit)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(LIST_OF_MAPS);

            List<Map<String, Object>> pullRequests = githubRestClient.get()
                .uri("/repos/{owner}/{repo}/pulls?state=all&per_page={limit}", owner, repo, safePullRequestLimit)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(LIST_OF_MAPS);

            return new RepositoryActivity(
                owner,
                repo,
                owner + "/" + repo,
                mapCommits(commits),
                mapPullRequests(pullRequests),
                OffsetDateTime.now(ZoneOffset.UTC)
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub 활동을 가져오지 못했습니다.", exception);
        }
    }

    private RepositoryActivity demoActivity(String owner, String repo, int commitLimit, int pullRequestLimit) {
        List<CommitSummary> commits = List.of(
            new CommitSummary("d34db33", "Add retrospective creation endpoint", "hyeonyeong", OffsetDateTime.now(ZoneOffset.UTC).minusDays(1), "https://github.com/%s/%s/commit/d34db33".formatted(owner, repo)),
            new CommitSummary("f00dbab", "Wire ranking page and share route", "hyeonyeong", OffsetDateTime.now(ZoneOffset.UTC).minusDays(2), "https://github.com/%s/%s/commit/f00dbab".formatted(owner, repo)),
            new CommitSummary("abc1234", "Introduce GitHub activity service", "hyeonyeong", OffsetDateTime.now(ZoneOffset.UTC).minusDays(3), "https://github.com/%s/%s/commit/abc1234".formatted(owner, repo)),
            new CommitSummary("42c0ffe", "Bootstrap Spring Boot backend", "hyeonyeong", OffsetDateTime.now(ZoneOffset.UTC).minusDays(4), "https://github.com/%s/%s/commit/42c0ffe".formatted(owner, repo))
        );
        List<PullRequestSummary> pullRequests = List.of(
            new PullRequestSummary(17L, "Create retrospective MVP flow", "hyeonyeong", "merged", OffsetDateTime.now(ZoneOffset.UTC).minusHours(8), "https://github.com/%s/%s/pull/17".formatted(owner, repo)),
            new PullRequestSummary(16L, "Add ranking page", "hyeonyeong", "closed", OffsetDateTime.now(ZoneOffset.UTC).minusDays(1), "https://github.com/%s/%s/pull/16".formatted(owner, repo)),
            new PullRequestSummary(15L, "Scaffold backend services", "hyeonyeong", "merged", OffsetDateTime.now(ZoneOffset.UTC).minusDays(2), "https://github.com/%s/%s/pull/15".formatted(owner, repo))
        );

        return new RepositoryActivity(
            owner,
            repo,
            owner + "/" + repo,
            commits.subList(0, Math.min(commitLimit, commits.size())),
            pullRequests.subList(0, Math.min(pullRequestLimit, pullRequests.size())),
            OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private List<CommitSummary> mapCommits(List<Map<String, Object>> commits) {
        List<CommitSummary> results = new ArrayList<>();
        if (commits == null) {
            return results;
        }

        for (Map<String, Object> commit : commits) {
            Map<String, Object> commitBlock = asMap(commit.get("commit"));
            Map<String, Object> authorBlock = asMap(commitBlock.get("author"));
            Map<String, Object> authorUser = asMap(commit.get("author"));

            results.add(new CommitSummary(
                asString(commit.get("sha")),
                asString(commitBlock.get("message")),
                firstNonBlank(asString(authorUser.get("login")), asString(authorBlock.get("name")), "unknown"),
                parseDate(asString(authorBlock.get("date"))),
                asString(commit.get("html_url"))
            ));
        }

        return results;
    }

    private List<PullRequestSummary> mapPullRequests(List<Map<String, Object>> pullRequests) {
        List<PullRequestSummary> results = new ArrayList<>();
        if (pullRequests == null) {
            return results;
        }

        for (Map<String, Object> pullRequest : pullRequests) {
            Map<String, Object> user = asMap(pullRequest.get("user"));

            results.add(new PullRequestSummary(
                asLong(pullRequest.get("number")),
                asString(pullRequest.get("title")),
                firstNonBlank(asString(user.get("login")), "unknown"),
                asString(pullRequest.get("state")),
                parseDate(asString(pullRequest.get("updated_at"))),
                asString(pullRequest.get("html_url"))
            ));
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(value);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private int normalizeLimit(int requestedLimit) {
        return Math.max(1, Math.min(requestedLimit, 20));
    }
}
