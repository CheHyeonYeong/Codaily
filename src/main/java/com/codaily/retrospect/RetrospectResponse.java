package com.codaily.retrospect;

import java.time.LocalDateTime;
import java.util.UUID;

public record RetrospectResponse(
    UUID id,
    String repoFullName,
    String title,
    String markdown,
    long viewCount,
    LocalDateTime createdAt,
    String shareUrl
) {
}
