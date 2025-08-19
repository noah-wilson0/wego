package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.plan.dto.TempTravelPlanAccommodationRequest;
import com.wego.wego.domain.plan.dto.TempTravelPlanPlaceRequest;
import com.wego.wego.domain.plan.dto.TravelPlanPlaceResponse;
import com.wego.wego.domain.plan.service.TravelPlanPlaceService;
import com.wego.wego.global.enums.PlaceType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 여행 일정 중 명소, 음석점, 카페, 숙소 관련 컨트롤러
 *
 */
@RestController
@RequestMapping("/travel_plan/place")
@RequiredArgsConstructor
public class TravelPlanPlaceController {

    private final TravelPlanPlaceService travelPlanPlaceService;

    @GetMapping("/{uuid}/{placeType}/paged")
    public ResponseEntity<Page<TravelPlanPlaceResponse>> getPagedPlaces(
            @PathVariable String uuid,
            @PathVariable String placeType,
            @PageableDefault(size=20, sort = {"averageRating", "likeCount"}, direction = Sort.Direction.DESC)Pageable pageable){

        Page<TravelPlanPlaceResponse> travelPlanPlaceResponses = travelPlanPlaceService.findAll(uuid, placeType, pageable);
        return ResponseEntity.ok(travelPlanPlaceResponses);
    }

    @PostMapping("/temp/schedule/{uuid}")
    public ResponseEntity<?> createTempPlace(@PathVariable String uuid, @RequestBody List<TempTravelPlanPlaceRequest> tempTravelPlanPlaceRequests){
        travelPlanPlaceService.saveTempSchedulePlace(uuid, tempTravelPlanPlaceRequests);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/temp/schedule/{uuid}/accommodation")
    public ResponseEntity<?> createTempAccommodation(@PathVariable String uuid, @RequestBody List<TempTravelPlanAccommodationRequest> tempTravelPlanAccommodationRequests){
        travelPlanPlaceService.saveTempScheduleAccommodation(uuid, tempTravelPlanAccommodationRequests);
        return ResponseEntity.ok().build();
    }


}
