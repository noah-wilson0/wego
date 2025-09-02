package com.wego.wego.domain.feed.service;

import com.wego.wego.domain.feed.dto.CommentRequest;
import com.wego.wego.domain.feed.dto.CommentResponse;
import com.wego.wego.domain.feed.entity.Comment;
import com.wego.wego.domain.feed.entity.Feed;
import com.wego.wego.domain.feed.repository.CommentRepository;
import com.wego.wego.domain.feed.repository.FeedRepository;
import com.wego.wego.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;

    @Transactional
    public void save(String feedId, CommentRequest commentRequest, Member member) {

        Feed feed = feedRepository.findById(Long.valueOf(feedId))
                .orElseThrow(() -> new RuntimeException("존재 하지 않는 피드"));

        Optional<Comment> optionalComment = Optional.ofNullable(commentRequest.parentId())
                .flatMap(commentRepository::findById);

        optionalComment.ifPresent(parent -> {
            if (!parent.getFeed().getId().equals(feed.getId())) {
                throw new IllegalArgumentException("부모 댓글은 동일한 피드에만 달 수 있습니다.");
            }
        });
        Comment comment;
        if (optionalComment.isPresent()) {
            comment = Comment.builder()
                    .feed(feed)
                    .member(member)
                    .parent(optionalComment.get())
                    .comment(commentRequest.comment())
                    .build();
        } else {
            comment = Comment.builder()
                    .feed(feed)
                    .member(member)
                    .comment(commentRequest.comment())
                    .build();
        }
        log.info("댓글 저장:{}",comment.toString());
        commentRepository.save(comment);

    }

    public Page<CommentResponse> getComments(String feedId, Pageable pageable) {
        return commentRepository.findByFeed_Id(Long.valueOf(feedId), pageable)
                .map(CommentResponse::from);
    }
}
