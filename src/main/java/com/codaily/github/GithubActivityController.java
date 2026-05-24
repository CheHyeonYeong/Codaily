package com.codaily.github;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/github")
public class GithubActivityController {

    private final GithubActivityService githubActivityService;

    public GithubActivityController(GithubActivityService githubActivityService) {
        this.githubActivityService = githubActivityService;
    }

    @GetMapping("/repositories/{owner}/{repo}/activity")
    public RepositoryActivity activity(
        @PathVariable String owner,
        @PathVariable String repo,
        @RequestParam(defaultValue = "5") @Min(1) @Max(20) int commitLimit,
        @RequestParam(defaultValue = "5") @Min(1) @Max(20) int pullRequestLimit,
        Authentication authentication
    ) {
        return githubActivityService.fetchActivity(owner, repo, commitLimit, pullRequestLimit, authentication);
    }
}
