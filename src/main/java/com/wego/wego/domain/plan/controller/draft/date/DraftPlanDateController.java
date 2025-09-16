package com.wego.wego.domain.plan.controller.draft.date;

import com.wego.wego.domain.plan.dto.TempTravelDateRequest;
import com.wego.wego.domain.plan.service.draft.date.TravelPlanDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 여행 일정 중 여행 날짜 생성, 여행 날짜별 시간 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/draft-plans")
@RequiredArgsConstructor
public class DraftPlanDateController {
    private final TravelPlanDateService travelPlanDateService;

    @PostMapping("/{uuid}/dates")
    public void createTempScheduleDate(@PathVariable  String uuid, @RequestBody TempTravelDateRequest tempTravelDateRequest) {
        if (tempTravelDateRequest == null) {
            log.info("travelDateRequest null");
        }
        log.info("createTempScheduleDate");
        travelPlanDateService.saveTempScheduleDate(uuid, tempTravelDateRequest);
    }
    @GetMapping("/{uuid}/dates")
    public ResponseEntity<String> getTempScheduleDate(@PathVariable  String uuid) {
        String scheduleDate = travelPlanDateService.getTempScheduleDate(uuid);
        return ResponseEntity.ok(scheduleDate);
    }
}
