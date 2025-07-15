package com.wego.wego.external.tourapi.config;


import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tourapi")
@Getter
public class TourApiProperties {
    private final String key;
    private final String baseurl;
    private final String location;
    private final String accommodation;
    private final String touristSpot;

    public TourApiProperties(String key, String baseurl, String location, String accommodation, String touristSpot) {
        this.key = key;
        this.baseurl = baseurl;
        this.location = location;
        this.accommodation = accommodation;
        this.touristSpot = touristSpot;
    }

}
