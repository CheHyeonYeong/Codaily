package com.codaily.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    RestClient githubRestClient(RestClient.Builder builder, CodailyProperties properties) {
        return builder
            .baseUrl(properties.getGithub().getApiBaseUrl())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "codaily-app")
            .build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
