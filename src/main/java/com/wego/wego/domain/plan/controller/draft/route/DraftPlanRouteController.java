package com.wego.wego.domain.plan.controller.draft.route;

import com.wego.wego.domain.plan.service.draft.route.TravelPlanRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 행 일정에서 여행 경로 생성을 담당하는 컨트롤러
 *
 */
@RestController
@RequestMapping("/draft-plans")
@RequiredArgsConstructor
public class DraftPlanRouteController {

    private final TravelPlanRouteService travelPlanRouteService;

    @PostMapping("/{uuid}/{route_type}/route")
    public ResponseEntity<?> createTempScheduleRoute(@PathVariable String route_type, @PathVariable String uuid) {
        travelPlanRouteService.saveScheduleRoute(uuid, route_type);
        return ResponseEntity.ok().build();
    }

}
