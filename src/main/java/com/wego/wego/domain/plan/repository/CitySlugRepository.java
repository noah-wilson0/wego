package com.wego.wego.domain.plan.repository;

import com.wego.wego.domain.plan.entity.CitySlug;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitySlugRepository extends JpaRepository<CitySlug, String> {

    @Query("select c.cityCodeId from CitySlug c where c.slug=:slug")
    List<Integer> findCityCodeIdsBySlug(@Param("slug") String slug);

    @Query("select c.cityCodeId from CitySlug c where c.label=:label")
    List<Integer> findCityCodeIdsByLabel(@Param("label") String label);

    @Query("select c.slug from CitySlug c where c.label = :label")
    Optional<String> findSlugByLabel(@Param("label") String label);

    @Query("select c.label from CitySlug c where c.slug = :slug")
    Optional<String> findLabelBySlug(@Param("slug") String slug);

}
