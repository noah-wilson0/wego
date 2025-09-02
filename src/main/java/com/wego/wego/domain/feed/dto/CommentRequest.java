package com.wego.wego.domain.feed.dto;

public record CommentRequest (
        Long parentId,
        String comment
){}
