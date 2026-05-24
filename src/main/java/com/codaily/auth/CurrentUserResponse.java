package com.codaily.auth;

public record CurrentUserResponse(
    boolean authenticated,
    boolean oauthEnabled,
    String name,
    String login,
    String loginUrl
) {
}
