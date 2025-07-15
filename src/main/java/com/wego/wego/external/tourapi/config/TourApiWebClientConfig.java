package com.wego.wego.external.tourapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * AreaCode 256k
 * TourSpot 30MB
 * Accommodation 10MB
 */
@Configuration
@RequiredArgsConstructor
public class TourApiWebClientConfig {
    private final TourApiProperties tourApiProperties;
    @Bean
    public WebClient tourApiWebClient() {

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(tourApiProperties.getBaseurl());
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        return WebClient.builder()
                .uriBuilderFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(clientCodecConfigurer -> clientCodecConfigurer
                                .defaultCodecs()
                                .maxInMemorySize(30*1024*1024))
                        .build())
                .build();
    }
}

