package com.wego.wego.domain.feed.controller;

import com.wego.wego.domain.feed.dto.FeedCreateRequest;
import com.wego.wego.domain.feed.dto.FeedInitResponse;
import com.wego.wego.domain.feed.dto.FeedResponse;
import com.wego.wego.domain.feed.service.FeedService;
import com.wego.wego.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    @GetMapping("/init/{travelPlanId}")
    public ResponseEntity<FeedInitResponse> init(@AuthenticationPrincipal Member member, @PathVariable String travelPlanId) {
        FeedInitResponse feedInitResponse = feedService.feedInit(member, travelPlanId);

        return ResponseEntity.ok(feedInitResponse);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createFeed(@AuthenticationPrincipal Member member,
                                        @RequestPart("payload") FeedCreateRequest feedCreateRequest,
                                        @RequestPart(value = "coverImage", required = false) MultipartFile coverImage) {

        feedService.saveFeed(member, feedCreateRequest, coverImage);

        return ResponseEntity.ok().build();

    }

    /**
     * 메인 화면 및 피드 페이지 피드 리스트 조회
     * @param pageable
     * @return
     */
    @GetMapping("/all/paged")

    public ResponseEntity<Page<FeedResponse>> getPagedFeeds(@PageableDefault(size=12, sort = {"likeCount", "viewCount"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedResponse> feedResponses = feedService.getAll(pageable);
        return ResponseEntity.ok(feedResponses);
    }

    /**
     * 피드 상세 페이지 조회
     * @param member
     * @param feed_id
     * @return
     */
    @GetMapping("/{feed_id}")
    public ResponseEntity<FeedResponse> getFeed(@AuthenticationPrincipal Member member, @PathVariable String feed_id) {
        FeedResponse feedResponse = feedService.getFeed(member, feed_id);

        return ResponseEntity.ok(feedResponse);
    }





}
