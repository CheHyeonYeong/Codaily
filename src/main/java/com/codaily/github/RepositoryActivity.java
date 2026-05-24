package com.codaily.github;

import java.time.OffsetDateTime;
import java.util.List;

public record RepositoryActivity(
    String repoOwner,
    String repoName,
    String repoFullName,
    List<CommitSummary> commits,
    List<PullRequestSummary> pullRequests,
    OffsetDateTime fetchedAt
) {
}
