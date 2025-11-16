package com.wego.wego.external.route.kakao.sevice;

import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.exception.KakaoRouteException;
import com.wego.wego.external.route.kakao.config.KaKaoProperties;
import com.wego.wego.external.route.kakao.dto.KaKaoMobilityResponse;
import com.wego.wego.external.route.kakao.utils.KaKaoCoordinateFormatter;
import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
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
/**
 * 이코드 완성시 EditPlanRepairContext는 삭제 해도 된다. @Recover 애노테이션 적용시 retryable에 사용된 파라미터를 그대로 가져올 수 있다.
 * 단, 파라미터 순서가 바뀌면 안된다.
 * url: https://developers.kakaomobility.com/docs/navi-api/reference/
 * 104 -> 이동시간 0분 처리
 * 105 -> kakao mobility api query param 에 roadevent:2로 해결 가능
 * 106 -> kakao mobility api query param 에 roadevent:2로 해결 가능
 *
 */

@Slf4j
@Service
public class KaKaoMobilityFetchService {

    @Qualifier("kaKaoMobilityWebClient")
    private final WebClient webClient;
    private final KaKaoProperties kaKaoProperties;

    // 필요하면 문서 보고 계속 보강하면 됨
    private static final Map<Integer, String> CODE_MEANING = Map.of(
            0, "SUCCESS" // (예) 0=성공
    );

    public KaKaoMobilityFetchService(
            @Qualifier("kaKaoMobilityWebClient") WebClient webClient,
            KaKaoProperties kaKaoProperties
    ) {
        this.webClient = webClient;
        this.kaKaoProperties = kaKaoProperties;
    }

    public RouteResult fetchKaKaoMobilityData(Place originPlace, Place destinationPlace) {
        final String originDbg = dbg(originPlace);
        final String destDbg   = dbg(destinationPlace);

        KaKaoCoordinateFormatter fmt = new KaKaoCoordinateFormatter();
        String origin = fmt.format(originPlace.getLongitude(), originPlace.getLatitude(), originPlace.getTitle());
        String destination = fmt.format(destinationPlace.getLongitude(), destinationPlace.getLatitude(), destinationPlace.getTitle());

        log.info("[Kakao] Request route: origin={}, destination={}", originDbg, destDbg);

        try {
            KaKaoMobilityResponse res = getKaKaoMobilityFromApi(origin, destination);

            if (res == null || res.routes() == null || res.routes().isEmpty()) {
                log.error("[Kakao] Empty routes. origin={}, destination={}", originDbg, destDbg);
                throw new RuntimeException("카카오 API 응답에 routes가 비었습니다");
            }

            KaKaoMobilityResponse.Route route = res.routes().get(0);
            int code = route.result_code();
            String msg = route.result_msg();

            /**
             * 104 code는 가까운 5m이내라서 경로 탐색 불가능함
             *  -> 0처리
             */
            if (code == 104) {
                return RouteResult.builder()
                        .originId(originPlace.getContentId())
                        .destinationId(destinationPlace.getContentId())
                        .fare(0)
                        .distance(0)
                        .duration(0)
                        .build();
            }
            /**
             * 105/106 error code는 출발지/목적지가 문제이므로 상위 클래스로 예외를 전파시키도록 함
             */
            else if (code == 105 || code == 106) {
                throw new KakaoRouteException(code, msg, originPlace.getContentId(), destinationPlace.getContentId());
            }
            else if (code != 0) {
                log.error("[Kakao] Route failed. result_code={}, meaning={}, result_msg={}, origin={}, destination={}",
                        code, CODE_MEANING.getOrDefault(code, "UNKNOWN"), msg, originDbg, destDbg);
                throw new RuntimeException("카카오 경로 요청 실패 (code=" + code + ", msg=" + msg + ")");
            }

            KaKaoMobilityResponse.Summary s = route.summary();
            log.info("[Kakao] Route OK. duration={}, distance={}, taxiFare={}, origin={}, destination={}",
                    s.duration(), s.distance(), s.fare() != null ? s.fare().taxi() : null, originDbg, destDbg);

            return RouteResult.builder()
                    .originId(originPlace.getContentId())
                    .destinationId(destinationPlace.getContentId())
                    .fare(s.fare() != null ? s.fare().taxi() : 0)
                    .distance(s.distance())
                    .duration(s.duration())
                    .build();

        } catch (RuntimeException e) {

            // 어떤 경우든 원점/목적지와 함께 로그
            log.error("[Kakao] Route exception. origin={}, destination={}, error={}", originDbg, destDbg, e.toString(), e);
            throw e;
        }
    }

//    // KaKaoMobilityFetchService
//    public List<RoutingSummary.RouteLeg> computeRemainingLegs(List<Place> chain, int startIdx) {
//        List<RoutingSummary.RouteLeg> tail = new ArrayList<>();
//        for (int i = startIdx; i < Math.max(0, chain.size() - 1); i++) {
//            RouteResult r = fetchKaKaoMobilityData(chain.get(i), chain.get(i + 1));
//            tail.add(RoutingSummary.RouteLeg.builder()
//                    .sequence(i + 1)
//                    .origin(r.getOriginId())
//                    .destination(r.getDestinationId())
//                    .duration(r.getDuration())
//                    .build());
//        }
//        return tail;
//    }


    private KaKaoMobilityResponse getKaKaoMobilityFromApi(String origin, String destination) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(kaKaoProperties.getRoute())
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .queryParam("roadevent",2)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[Kakao] HTTP error {}. origin={}, destination={}, body={}",
                                            resp.statusCode().value(), origin, destination, body);
                                    return Mono.error(new RuntimeException(
                                            "카카오 API HTTP 오류: " + resp.statusCode().value() + " / " + body));
                                })
                )
                .bodyToMono(KaKaoMobilityResponse.class)
                .block();
    }

    private static String dbg(Place p) {
        return String.format("id=%s,title=%s,lon=%s,lat=%s",
                p.getContentId(), p.getTitle(), p.getLongitude(), p.getLatitude());
    }
}
