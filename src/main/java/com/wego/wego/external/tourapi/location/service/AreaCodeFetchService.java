package com.wego.wego.external.tourapi.location.service;

import com.wego.wego.external.tourapi.config.TourApiProperties;
import com.wego.wego.external.tourapi.location.dto.LocationResponse;
import com.wego.wego.external.tourapi.location.entity.AreaCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Transactional
@Service
public class AreaCodeFetchService {
    private final WebClient webClient;
    private final TourApiProperties tourApiProperties;
    private final AreaCodeService areaCodeService;

    public AreaCodeFetchService(@Qualifier("tourApiWebClient") WebClient webClient, TourApiProperties tourApiProperties, AreaCodeService areaCodeService) {
        this.webClient = webClient;
        this.tourApiProperties = tourApiProperties;
        this.areaCodeService = areaCodeService;
    }

    public void fetchAreaCode() {
        LocationResponse response = getAreaCodeFromTourApi();

        if (response != null && response.getResponse() != null) {
            List<LocationResponse.CodeItem> areaCodeItems = response.getResponse().getBody().getItems().getItem();
            areaCodeItemsToDB(areaCodeItems);
        }
    }

    private LocationResponse getAreaCodeFromTourApi() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(tourApiProperties.getLocation())
                        .queryParam("numOfRows", 20)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "wego")
                        .queryParam("_type", "json")
                        .queryParam("serviceKey", tourApiProperties.getKey())
                        .build())
                .retrieve().bodyToMono(LocationResponse.class).block();
    }

    private void areaCodeItemsToDB(List<LocationResponse.CodeItem> items) {
        for (LocationResponse.CodeItem item : items) {
            Optional<AreaCode> existing = areaCodeService.findByName(item.getName());
            if (existing.isPresent()){
                AreaCode savedAreaCode = existing.get();
                if (!isEquals(item, savedAreaCode)) {
                    log.info("변경");
                    savedAreaCode.changeAll(savedAreaCode.getName(), savedAreaCode.getAreaCode());
                }
            }else{
                log.info("저장{}",item.toString());
                saveAreaCode(item.getName(), item.getCode());
            }

        }

    }
    private boolean isEquals(LocationResponse.CodeItem areaCode, AreaCode savedAreaCode) {
        return Objects.equals(areaCode.getCode(), savedAreaCode.getAreaCode())
                && Objects.equals(areaCode.getName(), savedAreaCode.getName());
    }
    private void saveAreaCode(String name, String code) {
        areaCodeService.save(new AreaCode(null, null,name, code));
    }
}
