package com.wego.wego.domain.feed.entity;

import com.wego.wego.domain.chemi.entity.Chemi;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "feed_chemi_tag")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class FeedChemiTag {

    @EmbeddedId
    private FeedChemiTagId id = new FeedChemiTagId();

    @MapsId("feedId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    @ToString.Exclude
    private Feed feed;

    @MapsId("chemiId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemi_id", nullable = false)
    @ToString.Exclude
    private Chemi chemi;


    public static FeedChemiTag of(Feed feed, Chemi chemi) {
        FeedChemiTag feedChemiTag = new FeedChemiTag();
        feedChemiTag.changeFeed(feed);
        feedChemiTag.changeChemi(chemi);

        return feedChemiTag;
    }

    public void changeFeed(Feed feed) {
        this.feed = feed;
    }
    public void changeChemi(Chemi chemi) {
        this.chemi = chemi;
    }


}
