package com.wego.wego.domain.plan.controller.draft.auto;

import com.wego.wego.domain.plan.dto.draft.auto.GeminiPlaceItemResponse;
import com.wego.wego.domain.plan.service.draft.auto.DraftPlanLangGraphService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/ai")
@RequiredArgsConstructor
public class GeminiPlaceItemController {
    private static final Logger log = LoggerFactory.getLogger(GeminiPlaceItemController.class);
    private final DraftPlanLangGraphService draftPlanLangGraphService;

    @GetMapping("/places")
    public ResponseEntity<Page<GeminiPlaceItemResponse>> getPlaces(
            @RequestParam String regionName,
            @RequestParam List<String> placeType,
            @PageableDefault(size=20, sort = {"averageRating", "likeCount"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<GeminiPlaceItemResponse> placesPaged = draftPlanLangGraphService.getPlacesPaged(regionName, placeType, pageable);
        log.info("mcp tool 사용됨");
        log.info("ai.getPlaces request region={}, types={}, page={}, size={}, sort={}",
                regionName, placeType, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return ResponseEntity.ok(placesPaged);
    }
}
