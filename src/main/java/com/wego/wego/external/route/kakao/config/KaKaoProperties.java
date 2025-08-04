package com.wego.wego.external.route.kakao.config;


import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakaomobility")
@Getter
public class KaKaoProperties {
    private final String basekey;
    private final String key;
    private final String baseurl;
    private final String route;

    public KaKaoProperties(String basekey, String key, String baseurl, String route) {
        this.basekey = basekey;
        this.key = key;
        this.baseurl = baseurl;
        this.route = route;
    }
}
