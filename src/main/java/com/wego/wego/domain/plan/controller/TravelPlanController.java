package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.plan.service.TravelPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("travel_plan")
@RequiredArgsConstructor
public class TravelPlanController {
    private final TravelPlanService travelPlanService;


    /**
     * 단순한 임시 여행 일정 조회
     * @param uuid
     * @return
     */
    @GetMapping("/temp/schedule/{uuid}")
    public ResponseEntity<String> getTempSchedule(@PathVariable String uuid) {
        String tempTravelPlan = travelPlanService.getTempTravelPlan(uuid);

        return ResponseEntity.ok().body(tempTravelPlan);

    }

    /**
     * TODO : 로그인 구현 후 시작하기
     * 저장 버튼 눌릴시 DB에 임시 여행일정 json을 db화
     * 하기 위해선 로그인이 되어야 한다.
     */
    @PostMapping("schedule/{uuid}")
    public ResponseEntity<Void> saveTravelPlan(@PathVariable String uuid) {


        return ResponseEntity.ok().build();
    }
    /**
     * 임시 조회 화면에서 저장을 눌릴시 db에 여행 일정이 저장되므로 db에서 읽는 것이 일반 조회
     * @param uuid
     * @return
     */
    @GetMapping("/schedule/{uuid}")
    public ResponseEntity<String> getSchedule(@PathVariable String uuid) {

        return ResponseEntity.ok().body(String.valueOf("qq"));
    }

}
