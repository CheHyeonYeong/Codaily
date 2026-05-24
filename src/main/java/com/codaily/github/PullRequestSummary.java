package com.codaily.github;

import java.time.OffsetDateTime;

public record PullRequestSummary(
    long number,
    String title,
    String authorName,
    String state,
    OffsetDateTime updatedAt,
    String url
) {
}
