package com.wego.wego.external.tourapi.place.service;

import com.wego.wego.external.tourapi.config.TourApiProperties;
import com.wego.wego.external.tourapi.location.entity.AreaCode;
import com.wego.wego.external.tourapi.location.entity.CityCode;
import com.wego.wego.external.tourapi.location.service.AreaCodeService;
import com.wego.wego.external.tourapi.location.service.CityCodeService;
import com.wego.wego.external.tourapi.place.dto.PlaceResponse;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.global.enums.PlaceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.util.Strings.isEmpty;

@Slf4j
@Transactional
@Service
public class PlaceFetchService {

    private final WebClient webClient;
    private final TourApiProperties tourApiProperties;
    private final AreaCodeService areaCodeService;
    private final CityCodeService cityCodeService;
    private final PlaceService placeService;

    public PlaceFetchService(@Qualifier("tourApiWebClient") WebClient webClient,
                             TourApiProperties tourApiProperties,
                             AreaCodeService areaCodeService,
                             CityCodeService cityCodeService,
                             PlaceService placeService) {
        this.webClient = webClient;
        this.tourApiProperties = tourApiProperties;
        this.areaCodeService = areaCodeService;
        this.cityCodeService = cityCodeService;
        this.placeService = placeService;
    }

    @Transactional
    public void fetchPlace() {
        List<String> targetTypes = List.of("12", "14", "15", "32", "38", "39");
        for (String placeType : targetTypes) {
            PlaceResponse placeResponse = getPlaceItemsFromApi(placeType);
            if (placeResponse != null && placeResponse.getResponse() != null) {
                List<PlaceResponse.PlaceItem> items = placeResponse.getResponse().getBody().getItems().getItem();
                tourSpotItemsToDB(items);
            }
        }
    }

    private PlaceResponse getPlaceItemsFromApi(String placeType) {
        return webClient.get().uri(uriBuilder -> uriBuilder
                        .path(tourApiProperties.getTouristSpot())
                        .queryParam("numOfRows", 52000)
                        .queryParam("pageNo", 1)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "wego")
                        .queryParam("_type", "json")
                        .queryParam("contentTypeId", placeType)
                        .queryParam("serviceKey", tourApiProperties.getKey())
                        .build())
                .retrieve()
                .bodyToMono(PlaceResponse.class)
                .block();
    }

    private void tourSpotItemsToDB(List<PlaceResponse.PlaceItem> placeItems) {
        List<String> contentIds = placeItems.stream()
                .map(PlaceResponse.PlaceItem::getContentid)
                .collect(Collectors.toList());

        Map<String, Place> existingPlaces = placeService.findAllByContentIdIn(contentIds)
                .stream()
                .collect(Collectors.toMap(Place::getContentId, Function.identity()));

        Map<String, AreaCode> areaCodeMap = areaCodeService.findAll()
                .stream()
                .collect(Collectors.toMap(AreaCode::getAreaCode, Function.identity()));

        Map<String, List<CityCode>> cityCodeMap = cityCodeService.findAll()
                .stream()
                .collect(Collectors.groupingBy(cityCode -> cityCode.getAreaCode().getAreaCode()));

        for (PlaceResponse.PlaceItem item : placeItems) {

            if (isEmpty(item.getAreacode()) || isEmpty(item.getSigungucode())) {
                log.warn("지역코드 또는 시군구코드 누락 title={}", item.getTitle());
                continue;
            }

            log.info(item.toString());

            Place existingPlace = existingPlaces.get(item.getContentid());
            AreaCode areaCode = areaCodeMap.get(item.getAreacode());
            CityCode cityCode = cityCodeMap.getOrDefault(item.getAreacode(), List.of())
                    .stream()
                    .filter(code -> code.getCityCode().equals(item.getSigungucode()))
                    .findFirst()
                    .orElse(null);

            if (areaCode == null || cityCode == null) {
                log.warn("AreaCode 또는 CityCode를 찾을 수 없습니다. title={}", item.getTitle());
                continue;
            }

            String rawTel = item.getTel();
            if (rawTel != null && rawTel.length() > 50) {
                rawTel = extractPhoneNumber(rawTel);
                item.changeTel(rawTel);
            }

            String typeCode = item.getContenttypeid();
            String finalType;
            if ("39".equals(typeCode)) {
                finalType = "A05020300".equals(item.getCat3()) ?
                        PlaceType.CAFE.getCode() : PlaceType.RESTAURANT.getCode();
            } else if ("32".equals(typeCode)) {
                finalType = PlaceType.ACCOMMODATION.getCode();
            } else {
                finalType = PlaceType.TOURIST_SPOT.getCode();
            }

            if (existingPlace != null) {
                if (!isEquals(item, existingPlace)) {
                    existingPlace.changeExceptAverageRatingAndLikeCount(
                            item.getContentid(), item.getTitle(),
                            cityCode, finalType,
                            item.getAddr1(), item.getAddr2(), item.getFirstimage(),
                            item.getMapx(), item.getMapy(), item.getTel()
                    );
                }
            } else {
                placeService.save(Place.builder()
                        .id(null)
                        .contentId(item.getContentid())
                        .title(item.getTitle())
                        .cityCode(cityCode)
                        .placeType(finalType)
                        .addr1(item.getAddr1())
                        .addr2(item.getAddr2())
                        .image(item.getFirstimage())
                        .longitude(item.getMapx())
                        .latitude(item.getMapy())
                        .tel(item.getTel())
                        .build());
            }
        }
    }

    private boolean isEquals(PlaceResponse.PlaceItem item, Place place) {
        return Objects.equals(item.getContentid(), place.getContentId()) &&
                Objects.equals(item.getTitle(), place.getTitle()) &&
                Objects.equals(item.getAddr1(), place.getAddr1()) &&
                Objects.equals(item.getAddr2(), place.getAddr2()) &&
                Objects.equals(item.getFirstimage(), place.getImage()) &&
                Objects.equals(item.getMapx(), place.getLongitude()) &&
                Objects.equals(item.getMapy(), place.getLatitude()) &&
                Objects.equals(item.getTel(), place.getTel());
    }

    public String extractPhoneNumber(String telRaw) {
        if (telRaw == null) return null;
        Matcher matcher = Pattern.compile("\\d{2,4}-\\d{3,4}-\\d{4}").matcher(telRaw);
        return matcher.find() ? matcher.group() : null;
    }
}
