package com.wego.wego.domain.plan.service.support;

import com.wego.wego.domain.plan.repository.AreaSlugRepository;
import com.wego.wego.domain.plan.repository.CitySlugRepository;
import com.wego.wego.external.tourapi.location.repository.CityCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SlugResolver {
    private final AreaSlugRepository areaSlugRepository;
    private final CitySlugRepository citySlugRepository;
    private final CityCodeRepository cityCodeRepository;

    public List<Integer> resolveCityIds(String slug) {
        return areaSlugRepository.findAreaCodeIdBySlug(slug)
                .map(cityCodeRepository::findCityCodeIdsByAreaCodeId)
                .orElseGet(() -> citySlugRepository.findCityCodeIdsBySlug(slug));
    }

    public String resolveLabel(String slug) {
        return areaSlugRepository.findLabelBySlug(slug)
                .orElseGet(() -> citySlugRepository.findLabelBySlug(slug)
                        .orElse("Unknown"));
    }
}
