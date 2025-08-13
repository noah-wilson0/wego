package com.wego.wego.domain.plan.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class CitySlugId implements Serializable {
    private String slug;
    private Integer cityCodeId;

    // equals/hashCode는 식별자 비교에 필수
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CitySlugId)) return false;
        CitySlugId that = (CitySlugId) o;
        return Objects.equals(slug, that.slug)
                && Objects.equals(cityCodeId, that.cityCodeId);
    }
    @Override public int hashCode() {
        return Objects.hash(slug, cityCodeId);
    }


}

