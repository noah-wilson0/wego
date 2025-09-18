package com.wego.wego.domain.plan.repository;

import com.wego.wego.domain.plan.entity.AreaSlug;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AreaSlugRepository extends JpaRepository<AreaSlug, String> {

    @Query("select a.areaCodeId from AreaSlug a where a.slug=:slug")
    Optional<Integer> findAreaCodeIdBySlug(@Param("slug") String slug);


    @Query("select a.label from AreaSlug a where a.slug = :slug")
    Optional<String> findLabelBySlug(@Param("slug") String slug);
}
