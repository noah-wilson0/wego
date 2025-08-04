package com.wego.wego.external.route.tmap.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmap")
@Getter
public class TMapTransitProperties {
    private final String key;
    private final String baseurl;
    private final String route;

    public TMapTransitProperties(String key, String baseurl, String route) {
        this.key = key;
        this.baseurl = baseurl;
        this.route = route;
    }
}
