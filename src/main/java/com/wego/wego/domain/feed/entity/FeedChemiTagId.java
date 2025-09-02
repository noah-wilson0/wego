package com.wego.wego.domain.feed.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class FeedChemiTagId implements Serializable {
    @Column(name = "feed_id")
    private Long feedId;

    @Column(name = "chemi_id")
    private Long chemiId;
}

