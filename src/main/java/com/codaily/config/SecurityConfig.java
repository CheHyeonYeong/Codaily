package com.codaily.config;

import java.util.List;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(
                    "/",
                    "/error",
                    "/h2-console/**",
                    "/login/**",
                    "/oauth2/**",
                    "/retrospects/**",
                    "/rankings",
                    "/actuator/health",
                    "/actuator/info",
                    "/api/auth/**",
                    "/api/rankings/**",
                    "/api/retrospects/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/github/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/retrospects").permitAll()
                .anyRequest().authenticated()
            );

        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(Customizer.withDefaults());
        }

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
