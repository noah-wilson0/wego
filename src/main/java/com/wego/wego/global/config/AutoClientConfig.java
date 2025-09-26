package com.wego.wego.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AutoClientConfig {

    @Bean
    public WebClient AutoWebClient(
            WebClient.Builder builder,
            @Value("${auto.base-url}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}
