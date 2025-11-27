package com.wego.wego.domain.plan.service.edit;

import com.wego.wego.domain.plan.dto.edit.route.EditPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.edit.route.EditPlanRoutingResponse;
import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.exception.KakaoRouteException;
import com.wego.wego.external.route.kakao.sevice.KaKaoMobilityFetchService;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 이거 완성 후 retry template 만들고 bean에 등록해놓고 다른 ai response에서 경로 계산시 retry가능하게 하면 된다.
 */
@Slf4j
@Service
public class EditPlanRoutingCalculator {

    private final KaKaoMobilityFetchService kakao;
    private final PlaceRepository placeRepository;
    @Qualifier("AutoWebClient")
    private final WebClient autoWebClient;

    public EditPlanRoutingCalculator(
            KaKaoMobilityFetchService kakao, PlaceRepository placeRepository,
            @Qualifier("AutoWebClient") WebClient autoWebClient) {
        this.kakao = kakao;
        this.placeRepository = placeRepository;
        this.autoWebClient = autoWebClient;

    }

    @Retryable(
            retryFor = KakaoRouteException.class,
            recover = "recoverEditPlan",
            maxAttempts = 2,
            backoff = @Backoff(delay = 300, multiplier = 2.0)
    )
    public EditPlanRoutingResponse calculateCarRoute(EditPlanRoutingRequest editPlanRoutingRequest) {
        Map<LocalDate, List<EditPlanRoutingResponse.RouteEdge>> dailyRoutes = new LinkedHashMap<>();
        for (int i = 0; i < editPlanRoutingRequest.getDays().size(); i++) {

            /*
             * 추후 아래 day별 경로 계산 및 retryable, cover는 EditPlanRoutingRepairService라든지
             * 적절한 class name을 가진 파일을 새로 만들어 모듈화 할 예정
             * */
            EditPlanRoutingRequest.DailyRouteRequest dailyRouteRequest = editPlanRoutingRequest.getDays().get(i);

            List<Place> dailyPlaces = dailyRouteRequest.getPlaces();

            List<EditPlanRoutingResponse.RouteEdge> routeEdges = new ArrayList<>();
            int dailyPlaceIndex=0;

            for (dailyPlaceIndex=0; dailyPlaceIndex < Math.max(0, dailyPlaces.size() - 1); dailyPlaceIndex++) {
                RouteResult routeResult = kakao.fetchKaKaoMobilityData(dailyPlaces.get(dailyPlaceIndex), dailyPlaces.get(dailyPlaceIndex + 1));
                routeEdges.add(
                        EditPlanRoutingResponse.RouteEdge.builder()
                                .sequence(dailyPlaceIndex + 1)
                                .origin(routeResult.getOriginId())
                                .destination(routeResult.getDestinationId())
                                .duration(routeResult.getDuration())
                                .build()
                );
            }

            dailyRoutes.put(dailyRouteRequest.getDate(), routeEdges);


        }

        return EditPlanRoutingResponse.builder()
                .routeType("car")
                .dailyRoutes(dailyRoutes)
                .build();

    }

    /**
     * 이코드 완성시 EditPlanRepairContext는 삭제 해도 된다. @Recover 애노테이션 적용시 retryable에 사용된 파라미터를 그대로 가져올 수 있다.
     * 단, 파라미터 순서가 바뀌면 안된다.
     * url: https://developers.kakaomobility.com/docs/navi-api/reference/
     * 104, 105, 106 error -> KaKaoMobilityFetchService docs 참고
     * @param ex
     */
    @Recover
    public EditPlanRoutingResponse recoverEditPlan(KakaoRouteException ex,
                                                   EditPlanRoutingRequest editPlanRoutingRequest) {
        log.warn("[EditPlanRoutingCalculator] Kakao route failed after retries. code={}, message={}",
                ex.getCode(), ex.getMessage());

        // ✅ fallback 전략 1: 날짜 정보만 유지하고, 각 날짜별 route 리스트는 빈 리스트로 반환
        Map<LocalDate, List<EditPlanRoutingResponse.RouteEdge>> fallbackDailyRoutes = new LinkedHashMap<>();

        for (EditPlanRoutingRequest.DailyRouteRequest day : editPlanRoutingRequest.getDays()) {
            // 이 날짜는 존재한다는 것만 보장, 실제 경로는 계산 실패
            fallbackDailyRoutes.put(day.getDate(), new ArrayList<>());
        }
        
        return EditPlanRoutingResponse.builder()
                .routeType("car")
                .dailyRoutes(fallbackDailyRoutes)
                .build();
    }



}
