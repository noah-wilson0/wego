package com.wego.wego.domain.feed.entity;

import com.wego.wego.domain.chemi.entity.Chemi;
import com.wego.wego.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Table(name = "feed")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class Feed {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false)
    @Builder.Default
    private String title="";

    @Builder.Default
    @Column(name = "people", nullable = false)
    private int people=1;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private int likeCount=0;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private int viewCount=0;

    @Column(name = "body", nullable = false)
    private String body;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt= LocalDate.now();

    @OneToOne(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private FeedTravelPlan feedTravelPlan;

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private Set<FeedChemiTag> feedChemiTags = new HashSet<>();

    public void changeFeedTravelPlan(FeedTravelPlan feedTravelPlan) {
        this.feedTravelPlan = feedTravelPlan;
        feedTravelPlan.changeFeed(this);
    }

    public void addChemi(Chemi chemi) {
        FeedChemiTag feedChemiTag = FeedChemiTag.of(this, chemi);
        feedChemiTags.add(feedChemiTag);
    }


}
