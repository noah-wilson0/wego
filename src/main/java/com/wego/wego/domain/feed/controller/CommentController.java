package com.wego.wego.domain.feed.controller;

import com.wego.wego.domain.feed.dto.CommentRequest;
import com.wego.wego.domain.feed.dto.CommentResponse;
import com.wego.wego.domain.feed.service.CommentService;
import com.wego.wego.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/feeds")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/{feed_id}/comments")
    public ResponseEntity<String> saveComment(@PathVariable("feed_id") String feedId,
                                         @RequestBody CommentRequest commentRequest, @AuthenticationPrincipal Member member) {
        commentService.save(feedId, commentRequest, member);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{feed_id}/comments")
    public ResponseEntity<Page<CommentResponse>> getComments(@PathVariable("feed_id") String feedId, @AuthenticationPrincipal Member member,
                                              @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
                                              Pageable pageable) {
        Page<CommentResponse> comments = commentService.getComments(feedId, pageable);
        log.info("getComments:{}",comments.toString());
        return ResponseEntity.ok().body(comments);
    }
}
