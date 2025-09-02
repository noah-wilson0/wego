package com.wego.wego.domain.feed.dto;

import com.wego.wego.domain.feed.entity.Comment;
import java.time.LocalDateTime;

public record CommentResponse(
        Long comment_id,
        Long parent_id,
        String author,
        String content,
        LocalDateTime created_at
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.getMember().getName(),   // 필요 시 getUsername 등으로 변경
                c.getComment(),
                c.getCreatedAt()
        );
    }
}

