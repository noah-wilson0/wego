package com.wego.wego.domain.place.controller;

import com.wego.wego.domain.place.dto.LabelSearchCondition;
import com.wego.wego.domain.place.dto.SearchCondition;
import com.wego.wego.domain.place.service.PlaceQueryService;
import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
import com.wego.wego.domain.plan.service.draft.place.TravelPlanPlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceQueryService placeQueryService;
    private final TravelPlanPlaceService travelPlanPlaceService;

    @GetMapping(path = "/search", params = "uuid")
    public ResponseEntity<Page<DraftPlanPlaceResponse>> search(SearchCondition condition,
                                    @PageableDefault(size=20, sort = {"averageRating", "likeCount"}, direction = Sort.Direction.DESC) Pageable pageable) {
        log.info(condition.toString());
        Page<DraftPlanPlaceResponse> search = placeQueryService.search(condition, pageable);

        return ResponseEntity.ok(search);
    }
    @GetMapping(path = "/search", params = "label")
    public ResponseEntity<Page<DraftPlanPlaceResponse>> searchByLabel(LabelSearchCondition condition,
                                                               @PageableDefault(size=20, sort = {"averageRating", "likeCount"}, direction = Sort.Direction.DESC) Pageable pageable) {
        log.info(condition.toString());
        Page<DraftPlanPlaceResponse> search = placeQueryService.searchByLabel(condition, pageable);

        return ResponseEntity.ok(search);
    }

    /** 여행 일정 편집 - 장소보관함 추가 화면 - 지역(label) 기반 장소 페이징 조회
     *
     * @param placeType
     * @param pageable
     * @return
     */
    @GetMapping("/{label}/{placeType}/paged")
    public ResponseEntity<Page<DraftPlanPlaceResponse>> getPagedPlacesByLabel(
            @PathVariable String label,
            @PathVariable String placeType,
            @PageableDefault(size=20, sort = {"averageRating", "likeCount"}, direction = Sort.Direction.DESC)Pageable pageable){

        Page<DraftPlanPlaceResponse> travelPlanPlaceResponses = travelPlanPlaceService.findAllByLabel(label, placeType, pageable);
        return ResponseEntity.ok(travelPlanPlaceResponses);
    }


}
