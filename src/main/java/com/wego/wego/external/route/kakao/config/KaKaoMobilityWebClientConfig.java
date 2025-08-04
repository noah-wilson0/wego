package com.wego.wego.external.route.kakao.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
@RequiredArgsConstructor
public class KaKaoMobilityWebClientConfig {
    private final KaKaoProperties kaKaoProperties;

    @Bean
    public WebClient kaKaoMobilityWebClient() {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(kaKaoProperties.getBaseurl());
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        return WebClient.builder()
                .uriBuilderFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        kaKaoProperties.getBasekey()+kaKaoProperties.getKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(clientCodecConfigurer -> clientCodecConfigurer
                                .defaultCodecs()
                                .maxInMemorySize(30*1024*1024))
                        .build())
                .build();
    }
}
