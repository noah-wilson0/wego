package com.wego.wego.domain.plan.repository;

import com.wego.wego.domain.plan.entity.CitySlug;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitySlugRepository extends JpaRepository<CitySlug, String> {

    @Query("select c.cityCodeId from CitySlug c where c.slug=:slug")
    List<Integer> findCityCodeIdsBySlug(@Param("slug") String slug);


}
