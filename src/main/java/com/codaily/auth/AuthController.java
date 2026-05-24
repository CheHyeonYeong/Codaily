package com.codaily.auth;

import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final boolean oauthEnabled;

    public AuthController(ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
        ClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();
        this.oauthEnabled = repository instanceof InMemoryClientRegistrationRepository;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        if (
            authentication == null ||
            authentication instanceof AnonymousAuthenticationToken ||
            !authentication.isAuthenticated()
        ) {
            return new CurrentUserResponse(false, oauthEnabled, null, null, oauthEnabled ? "/oauth2/authorization/github" : null);
        }

        String login = null;
        String name = authentication.getName();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
            Object loginValue = attributes.get("login");
            Object nameValue = attributes.get("name");
            login = loginValue == null ? null : loginValue.toString();
            if (nameValue != null && !nameValue.toString().isBlank()) {
                name = nameValue.toString();
            }
        }

        return new CurrentUserResponse(true, oauthEnabled, name, login, oauthEnabled ? "/oauth2/authorization/github" : null);
    }
}
