package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.plan.dto.TempTravelDateRequest;
import com.wego.wego.domain.plan.dto.TempTravelTimeRequest;
import com.wego.wego.domain.plan.service.TravelPlanDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 여행 일정 중 여행 날짜 생성, 여행 날짜별 시간 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("travel_plan/date")
@RequiredArgsConstructor
public class TravelPlanDateController {
    private final TravelPlanDateService travelPlanDateService;

    @PostMapping("/temp/schedule/{uuid}")
    public void createTempScheduleDate(@PathVariable  String uuid, @RequestBody TempTravelDateRequest tempTravelDateRequest) {
        if (tempTravelDateRequest == null) {
            log.info("travelDateRequest null");
        }
        travelPlanDateService.saveTempScheduleDate(uuid, tempTravelDateRequest);
    }
    @GetMapping("/temp/schedule/{uuid}")
    public ResponseEntity<String> getTempScheduleDate(@PathVariable  String uuid) {
        String scheduleDate = travelPlanDateService.getTempScheduleDate(uuid);
        return ResponseEntity.ok(scheduleDate);
    }

    @PostMapping("/temp/schedule/times/{uuid}")
    public void createTempScheduleTimes(@PathVariable  String uuid, @RequestBody TempTravelTimeRequest tempTravelTimeRequest) {
        travelPlanDateService.saveTempScheduleTime(uuid, tempTravelTimeRequest);
    }
    @GetMapping("/temp/schedule/times/{uuid}")
    public ResponseEntity<String> getTempScheduleTimes(@PathVariable  String uuid) {
        String scheduleTime = travelPlanDateService.getTempScheduleTime(uuid);
        return ResponseEntity.ok(scheduleTime);
    }
}
