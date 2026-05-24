package com.codaily.retrospect;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRetrospectRequest(
    @NotBlank String owner,
    @NotBlank String repo,
    Integer commitLimit,
    Integer pullRequestLimit,
    @Size(max = 400) String focus,
    @Size(max = 30) String aiProvider,
    @Size(max = 100) String aiModel
) {

    public int resolvedCommitLimit() {
        return commitLimit == null ? 5 : commitLimit;
    }

    public int resolvedPullRequestLimit() {
        return pullRequestLimit == null ? 5 : pullRequestLimit;
    }
}
