package com.codaily.github;

import java.time.OffsetDateTime;

public record CommitSummary(
    String sha,
    String message,
    String authorName,
    OffsetDateTime committedAt,
    String url
) {
}
