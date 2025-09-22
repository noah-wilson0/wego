package com.wego.wego.domain.place.controller;

import com.wego.wego.domain.place.dto.SearchCondition;
import com.wego.wego.domain.place.service.PlaceQueryService;
import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceQueryService placeQueryService;

    @GetMapping("/search")
    public ResponseEntity<Page<DraftPlanPlaceResponse>> search(SearchCondition condition,
                                    @PageableDefault(size=20, sort = {"averageRating", "likeCount"}, direction = Sort.Direction.DESC) Pageable pageable) {
        log.info(condition.toString());
        Page<DraftPlanPlaceResponse> search = placeQueryService.search(condition, pageable);

        return ResponseEntity.ok(search);
    }

}
