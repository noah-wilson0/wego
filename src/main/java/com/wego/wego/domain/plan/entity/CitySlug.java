package com.wego.wego.domain.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="city_slug")
@IdClass(CitySlugId.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CitySlug {

    @Id
    @Column(name = "slug")
    private String slug;

    @Column(name = "label",nullable = false)
    private String label;

    @Id
    @Column(name = "city_code_id")
    private Integer cityCodeId;
}
