package com.wego.wego.external.tourapi.location.service;

import com.wego.wego.external.tourapi.config.TourApiProperties;
import com.wego.wego.external.tourapi.location.dto.LocationResponse;
import com.wego.wego.external.tourapi.location.entity.AreaCode;
import com.wego.wego.external.tourapi.location.entity.CityCode;
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
public class CityCodeFetchService {


    private final WebClient webClient;
    private final TourApiProperties tourApiProperties;
    private final AreaCodeService areaCodeService;
    private final CityCodeService cityCodeService;

    public CityCodeFetchService(@Qualifier("tourApiWebClient")WebClient webClient, TourApiProperties tourApiProperties, AreaCodeService areaCodeService, CityCodeService cityCodeService) {
        this.webClient = webClient;
        this.tourApiProperties = tourApiProperties;
        this.areaCodeService = areaCodeService;
        this.cityCodeService = cityCodeService;
    }

    @Transactional
    public void fetchCityCodes(){
        List<AreaCode> areaCodes = areaCodeService.findAll();

        for (AreaCode areaCode : areaCodes) {

            LocationResponse response = getCityCodeFromTourApi(areaCode.getAreaCode());

            if (response != null && response.getResponse() != null) {
                List<LocationResponse.CodeItem> items =
                        response.getResponse().getBody().getItems().getItem();
                cityCodeItemsToDB(items, areaCode.getAreaCode());
            }
        }

    }

    private LocationResponse getCityCodeFromTourApi(String areaCode) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(tourApiProperties.getLocation())
                        .queryParam("numOfRows", 50)
                        .queryParam("pageNo",1)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("areaCode",areaCode)
                        .queryParam("MobileApp", "wego")
                        .queryParam("_type", "json")
                        .queryParam("serviceKey", tourApiProperties.getKey())
                        .build())
                .retrieve()
                .bodyToMono(LocationResponse.class)
                .block();
    }

    private void cityCodeItemsToDB(List<LocationResponse.CodeItem> items, String areaCode) {
        for (LocationResponse.CodeItem item : items) {
            Optional<AreaCode> findAreaCode = areaCodeService.findByAreaCode(areaCode);
            Optional<CityCode> findCityCode = cityCodeService.findByCityCodeAndAreaCodeId(item.getCode(), findAreaCode.get().getAreaCodeId());
            if (findCityCode.isPresent()){
                CityCode savedCityCode = findCityCode.get();
                if (!isEquals(item, savedCityCode)) {
                    savedCityCode.changeAll(savedCityCode.getName(), savedCityCode.getCityCode());
                }
            }else{
                saveCityCode(item.getName(), item.getCode(), areaCode);
            }

        }

    }

    private boolean isEquals(LocationResponse.CodeItem ciTyCode, CityCode savedCityCode) {
        return Objects.equals(ciTyCode.getCode(), savedCityCode.getCityCode())
                && Objects.equals(ciTyCode.getName(), savedCityCode.getName());
    }

    private void saveCityCode(String name, String cityCode, String areaCode) {
        Optional<AreaCode> findAreaCode = areaCodeService.findByAreaCode(areaCode);

        if (findAreaCode.isEmpty()) {
            log.warn("AreaCode가 존재하지 않아 CityCode 저장을 생략함: name={}, code={}", name, cityCode);
            return;
        }

        cityCodeService.save(CityCode.createCityCode(cityCode, name, findAreaCode.get()));
    }
}
