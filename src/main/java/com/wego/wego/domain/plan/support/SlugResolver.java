package com.wego.wego.domain.plan.support;

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

    public String resolveSlugByLabel(String label) {
        return areaSlugRepository.findSlugByLabel(label)
                .orElseGet(() -> citySlugRepository.findSlugByLabel(label)
                        .orElse("Unknown"));
    }

    public String resolveLabel(String slug) {
        return areaSlugRepository.findLabelBySlug(slug)
                .orElseGet(() -> citySlugRepository.findLabelBySlug(slug)
                        .orElse("Unknown"));
    }
    public List<Integer> resolveCityIdsByLabel(String label) {
        return areaSlugRepository.findAreaCodeIdByLabel(label)
                .map(cityCodeRepository::findCityCodeIdsByAreaCodeId)
                .orElseGet(() -> citySlugRepository.findCityCodeIdsByLabel(label));
    }
}
