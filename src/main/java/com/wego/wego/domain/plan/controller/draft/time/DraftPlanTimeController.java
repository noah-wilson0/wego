package com.wego.wego.domain.plan.controller.draft.time;

import com.wego.wego.domain.plan.dto.TempTravelTimeRequest;
import com.wego.wego.domain.plan.service.draft.time.DraftPlanTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *  여행 날짜별 시간 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/draft-plans")
@RequiredArgsConstructor
public class DraftPlanTimeController {
    private final DraftPlanTimeService draftPlanTimeService;
    @PostMapping("/{uuid}/times")
    public void createTempScheduleTimes(@PathVariable String uuid, @RequestBody TempTravelTimeRequest tempTravelTimeRequest) {
        draftPlanTimeService.saveTempScheduleTime(uuid, tempTravelTimeRequest);
    }
    @GetMapping("/{uuid}/times")
    public ResponseEntity<String> getTempScheduleTimes(@PathVariable  String uuid) {
        String scheduleTime = draftPlanTimeService.getTempScheduleTime(uuid);
        return ResponseEntity.ok(scheduleTime);
    }
}
