package com.wego.wego.domain.plan.controller.draft.auto;

import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ai mcp request api
 * 여행 일정 중 명소, 음석점, 카페, 숙소 관련 컨트롤러
 *
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GeminiPlaceItemController {
    private static final Logger log = LoggerFactory.getLogger(GeminiPlaceItemController.class);
    private final DraftPlanLangGraphService draftPlanLangGraphService;


    /**
     * ai가 모든 데이터를 읽으려고 하니 지연 및 효율성 저하로 변경 예정
     * @param regionName
     * @param placeType
     * @param pageable
     * @return
     */
    @GetMapping("v1/ai/places")
    public ResponseEntity<Page<GeminiPlaceItemResponse>> getPlaces(
            @RequestParam String regionName,
            @RequestParam List<String> placeType,
            @PageableDefault(size=20, sort = {"averageRating", "likeCount"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<GeminiPlaceItemResponse> placesPaged = draftPlanLangGraphService.getPlacesPaged(regionName, placeType, pageable);
        log.info("v1/ai/places 사용됨");
        log.info("ai.getPlaces request region={}, types={}, page={}, size={}, sort={}",
                regionName, placeType, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return ResponseEntity.ok(placesPaged);
    }


    @GetMapping("v2/ai/places")
    public ResponseEntity<List<GeminiPlaceItemResponse>> getPlacesV2(
            @RequestParam String regionName,
            @RequestParam List<String> placeType,
            @RequestParam String title) {
        log.info("RequestParam: region={}, types={}, title={}", regionName, placeType, title);
        List<GeminiPlaceItemResponse> search = draftPlanLangGraphService.searchPlace(regionName, placeType, title);
        log.info("v2/ai/places 사용됨");
        log.info("searchItems(size={}): [{}]",
                search.size(),
                search.stream()
                        .map(it -> it.getTitle() + " | " + it.getAddr() + " | " + it.getTel())
                        .collect(Collectors.joining(", "))
        );
        return ResponseEntity.ok(search);
    }
}
