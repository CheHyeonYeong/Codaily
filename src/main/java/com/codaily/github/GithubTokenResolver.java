package com.codaily.github;

import com.codaily.config.CodailyProperties;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GithubTokenResolver {

    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider;
    private final CodailyProperties properties;

    public GithubTokenResolver(
        ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider,
        CodailyProperties properties
    ) {
        this.authorizedClientServiceProvider = authorizedClientServiceProvider;
        this.properties = properties;
    }

    public Optional<String> resolve(Authentication authentication) {
        if (StringUtils.hasText(properties.getGithub().getPersonalAccessToken())) {
            return Optional.of(properties.getGithub().getPersonalAccessToken());
        }

        OAuth2AuthorizedClientService authorizedClientService = authorizedClientServiceProvider.getIfAvailable();
        if (authorizedClientService == null || !(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            return Optional.empty();
        }

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthToken.getName()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return Optional.empty();
        }

        String tokenValue = authorizedClient.getAccessToken().getTokenValue();
        return StringUtils.hasText(tokenValue) ? Optional.of(tokenValue) : Optional.empty();
    }
}
