package com.wego.wego.external.route.kakao.sevice;

import com.wego.wego.external.route.kakao.config.KaKaoProperties;
import com.wego.wego.external.route.kakao.dto.KaKaoMobilityResponse;
import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.kakao.utils.KaKaoCoordinateFormatter;
import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 *
    * 일정 데이터는
    * 장소 json: [{"contentId":"2930839"},{"contentId":"2763773"},{"contentId":"1959485"}]
    * 숙소 json: [{"contentId":"3102105"}]
    *
    * 일정별 최소 장소 1개를 만족한다.
    * 일정 -1만큼 마지막 노드는 숙소가 추가된다.
    *  이를 만족하는 객체를 만든 후 이것을 getCarKaKaoMobility에 넣고 루프를 돌려서 장소쌍마다 fetchKaKaoMobilityData을 돌린다.
 */
@Slf4j
@Service
public class KaKaoMobilityFetchService {
    @Qualifier("kaKaoMobilityWebClient")
    private final WebClient webClient;

    private final KaKaoProperties kaKaoProperties;

    public KaKaoMobilityFetchService(
            @Qualifier("kaKaoMobilityWebClient") WebClient webClient,
            KaKaoProperties kaKaoProperties
    ) {
        this.webClient = webClient;
        this.kaKaoProperties = kaKaoProperties;
    }


    public RouteResult fetchKaKaoMobilityData(Place originPlace, Place destinationPlace) {
        log.info("originPlace:{%s}, {%s}".formatted(originPlace.getLongitude(),originPlace.getLatitude()));
        log.info("destinationPlace:{%s}, {%s}".formatted(destinationPlace.getLongitude(),destinationPlace.getLatitude()));
        KaKaoCoordinateFormatter kaKaoCoordinateFormatter = new KaKaoCoordinateFormatter();
        String origin = kaKaoCoordinateFormatter.format(originPlace.getLongitude(), originPlace.getLatitude(), originPlace.getTitle());
        String destination = kaKaoCoordinateFormatter.format(destinationPlace.getLongitude(), destinationPlace.getLatitude(), destinationPlace.getTitle());

        try {
            KaKaoMobilityResponse kaKaoMobilityResponse = getKaKaoMobilityFromApi(origin, destination);
            log.info("경로 생성 완료 ");
            if (kaKaoMobilityResponse.routes() != null && !kaKaoMobilityResponse.routes().isEmpty()) {
                KaKaoMobilityResponse.Route route = kaKaoMobilityResponse.routes().get(0);
                if (route.result_code() == 0) {
                    log.info("경로 반환 성공");
                    KaKaoMobilityResponse.Summary summary = route.summary();
                    return RouteResult.builder()
                            .originId(originPlace.getContentId())
                            .destinationId(destinationPlace.getContentId())
                            .fare(summary.fare().taxi())
                            .distance(summary.distance())
                            .duration(summary.duration()).build();
                } else {
                    throw new RuntimeException("카카오 경로 요청 실패");
                }
            } else {
                throw new RuntimeException("카카오 API 응답에 routes가 비어 있습니다.");
            }
        }catch (RuntimeException e) {
            throw new RuntimeException("카카오 경로 요청 중 오류 발생", e);
        }

    }

    private KaKaoMobilityResponse getKaKaoMobilityFromApi(String origin, String destination) {
        log.info("fetchRouteFromApi: origin={}, destination={}", origin, destination);
        KaKaoMobilityResponse response = null;
        try {
            response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(kaKaoProperties.getRoute())
                            .queryParam("origin", origin)
                            .queryParam("destination", destination)
                            .build())
                    .retrieve()
                    .bodyToMono(KaKaoMobilityResponse.class)
                    .block();
        } catch (Exception e) {
            log.info("요청 에러");
            throw new RuntimeException(e);
        }
        log.info("결과 코드:"+response.routes().get(0).result_code());
        return response;

    }

}
