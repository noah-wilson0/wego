package com.wego.wego.domain.feed.repository;

import com.wego.wego.domain.feed.entity.Feed;
import com.wego.wego.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {

    Optional<Feed> findByIdAndMember(Long id, Member member);
}

