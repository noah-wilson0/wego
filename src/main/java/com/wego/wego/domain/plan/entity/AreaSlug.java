package com.wego.wego.domain.plan.entity;

import com.wego.wego.external.tourapi.location.entity.AreaCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

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
}
