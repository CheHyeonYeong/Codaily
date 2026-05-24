package com.codaily.retrospect;

import java.time.LocalDateTime;
import java.util.UUID;

public record RetrospectSummaryResponse(
    UUID id,
    String repoFullName,
    String title,
    long viewCount,
    LocalDateTime createdAt,
    String shareUrl
) {
}
