package com.wego.wego.external.route.tmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.tmap.config.TMapTransitProperties;
import com.wego.wego.external.route.tmap.dto.TMapTransitRequest;
import com.wego.wego.external.route.tmap.dto.TMapTransitResponse;
import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class TMapTransitService {
    @Qualifier("TMapTransitWebClient")
    private final WebClient webClient;

    private final TMapTransitProperties tMapTransitProperties;

    private final ObjectMapper objectMapper;

    public TMapTransitService(@Qualifier("TMapTransitWebClient") WebClient webClient, TMapTransitProperties tMapTransitProperties, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.tMapTransitProperties = tMapTransitProperties;
        this.objectMapper = objectMapper;
    }

    public RouteResult fetchTMapTransitData(Place originPlace, Place destinationPlace) {
        TMapTransitResponse tmapTransitResponse;

        try {
            tmapTransitResponse = getTMapTransitFromApi(
                    objectMapper.writeValueAsString(
                    new TMapTransitRequest(originPlace.getLongitude()
                            , originPlace.getLatitude(),
                            destinationPlace.getLongitude(),
                            destinationPlace.getLatitude(),
                            1)
                    )

            );
            if (tmapTransitResponse != null) {
                log.info("🧾 실제 응답 바디: {}", tmapTransitResponse);
            }else{
                log.warn("🚨 TMap API 응답이 비어있음: {}", tmapTransitResponse);
                log.info("TMap API 응답에 metaData가 없습니다");
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


        return new RouteResult(
                originPlace.getContentId(),
                destinationPlace.getContentId(),
                tmapTransitResponse.metaData().plan().itineraries().getFirst().fare().regular().totalFare(),
                tmapTransitResponse.metaData().plan().itineraries().getFirst().totalDistance(),
                tmapTransitResponse.metaData().plan().itineraries().getFirst().totalTime()
        );
    }

    /** TODO: 왜 2번쨰 요청에서  tmapTransitResponse.metadata=null인지 파악해야됨
     * api에서 10번이상 요청하면 429 Too Many Requests 에러를 응답한다.
     * @param req
     * @return
     */
    private TMapTransitResponse getTMapTransitFromApi(String req) {


        log.info("📨 요청 바디: {}", req);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("")
                        .build())
                .bodyValue(req)
                .retrieve()
                .bodyToMono(TMapTransitResponse.class)
                .block();
//        String responseBody = webClient.post()
//                .uri(uriBuilder -> uriBuilder
//                        .path("")
//                        .build())
//                .bodyValue(req)
//                .retrieve()
//                .bodyToMono(String.class)
//                .block(); // 응답을 문자열로 먼저 받음
//
//        log.info("🧾 원본 응답 바디: {}", responseBody); // 👉 여기가 핵심
// 이제 ObjectMapper로 매핑
//        ObjectMapper objectMapper = new ObjectMapper();
//        TMapTransitResponse parsed = null;
//        try {
//            parsed = objectMapper.readValue(responseBody, TMapTransitResponse.class);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        return parsed;
    }

}
