package com.wego.wego.domain.plan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="area_slug")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AreaSlug {

    @Id
    @Column(name = "slug")
    private String slug;

    @Column(name = "label",nullable = false)
    private String label;

    @Column(name = "area_code_id")
    private Integer  areaCodeId;

//    @OneToMany(mappedBy = "areaSlug")
//    private List<TravelPlan> travelPlans=new ArrayList<>();
}
