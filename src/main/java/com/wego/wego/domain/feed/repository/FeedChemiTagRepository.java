package com.wego.wego.domain.feed.repository;

import com.wego.wego.domain.feed.entity.FeedChemiTag;
import com.wego.wego.domain.feed.entity.FeedChemiTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedChemiTagRepository extends JpaRepository<FeedChemiTag, FeedChemiTagId> {
}
