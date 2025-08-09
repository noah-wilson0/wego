package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.plan.dto.TempTravelDateRequest;
import com.wego.wego.domain.plan.dto.TempTravelTimeRequest;
import com.wego.wego.domain.plan.service.TravelPlanDayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 여행 일정 중 여행 날짜 생성, 여행 날짜별 시간 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/travel_plan/date")
@RequiredArgsConstructor
public class TravelPlanDayController {
    private final TravelPlanDayService travelPlanDayService;

    @PostMapping("/temp/schedule/{uuid}")
    public void createTempScheduleDate(@PathVariable  String uuid, @RequestBody TempTravelDateRequest tempTravelDateRequest) {
        if (tempTravelDateRequest == null) {
            log.info("travelDateRequest null");
        }
        log.info("createTempScheduleDate");
        travelPlanDayService.saveTempScheduleDate(uuid, tempTravelDateRequest);
    }
    @GetMapping("/temp/schedule/{uuid}")
    public ResponseEntity<String> getTempScheduleDate(@PathVariable  String uuid) {
        String scheduleDate = travelPlanDayService.getTempScheduleDate(uuid);
        return ResponseEntity.ok(scheduleDate);
    }

    @PostMapping("/temp/schedule/times/{uuid}")
    public void createTempScheduleTimes(@PathVariable  String uuid, @RequestBody TempTravelTimeRequest tempTravelTimeRequest) {
        travelPlanDayService.saveTempScheduleTime(uuid, tempTravelTimeRequest);
    }
    @GetMapping("/temp/schedule/times/{uuid}")
    public ResponseEntity<String> getTempScheduleTimes(@PathVariable  String uuid) {
        String scheduleTime = travelPlanDayService.getTempScheduleTime(uuid);
        return ResponseEntity.ok(scheduleTime);
    }
}
