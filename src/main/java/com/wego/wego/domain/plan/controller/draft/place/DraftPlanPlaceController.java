package com.wego.wego.domain.plan.controller.draft.place;

import com.wego.wego.domain.plan.dto.TempTravelPlanAccommodationRequest;
import com.wego.wego.domain.plan.dto.TempTravelPlanPlaceRequest;
import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
import com.wego.wego.domain.plan.service.draft.place.TravelPlanPlaceService;
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
@RequestMapping("/draft-plans")
@RequiredArgsConstructor
public class DraftPlanPlaceController {

    private final TravelPlanPlaceService travelPlanPlaceService;

    /**
     * 여행 장소/숙소 페이지 조회 -> /places로 병합 예정
     * @param uuid
     * @param placeType
     * @param pageable
     * @return
     */
    @GetMapping("/{uuid}/{placeType}/paged")
    public ResponseEntity<Page<DraftPlanPlaceResponse>> getPagedPlaces(
            @PathVariable String uuid,
            @PathVariable String placeType,
            @PageableDefault(size=20, sort = {"averageRating", "likeCount"}, direction = Sort.Direction.DESC)Pageable pageable){

        Page<DraftPlanPlaceResponse> travelPlanPlaceResponses = travelPlanPlaceService.findAll(uuid, placeType, pageable);
        return ResponseEntity.ok(travelPlanPlaceResponses);
    }



    @PostMapping("/{uuid}/places")
    public ResponseEntity<?> createTempPlace(@PathVariable String uuid, @RequestBody List<TempTravelPlanPlaceRequest> tempTravelPlanPlaceRequests){
        travelPlanPlaceService.saveTempSchedulePlace(uuid, tempTravelPlanPlaceRequests);
        return ResponseEntity.ok().build();
    }

    /**
     * 프론트에서 자체적으로 마지막날 숙소를 제외한 상태로 들어옴
     * @param uuid
     * @param tempTravelPlanAccommodationRequests
     * @return
     */
    @PostMapping("/{uuid}/accommodations")
    public ResponseEntity<?> createTempAccommodation(@PathVariable("uuid") String uuid, @RequestBody List<TempTravelPlanAccommodationRequest> tempTravelPlanAccommodationRequests){
        travelPlanPlaceService.saveTempScheduleAccommodation(uuid, tempTravelPlanAccommodationRequests);
        return ResponseEntity.ok().build();
    }


}
