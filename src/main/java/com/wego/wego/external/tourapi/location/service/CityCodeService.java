package com.wego.wego.external.tourapi.location.service;

import com.wego.wego.external.tourapi.location.entity.AreaCode;
import com.wego.wego.external.tourapi.location.entity.CityCode;
import com.wego.wego.external.tourapi.location.repository.CityCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CityCodeService {
    private final CityCodeRepository cityCodeRepository;

    public CityCode save(CityCode cityCode) {
        return cityCodeRepository.save(cityCode);
    }

    public Optional<CityCode> findByCode(String cityCode) {
        return cityCodeRepository.findByCityCode(cityCode);
    }
    public Optional<CityCode> findByName(String name) {
        return cityCodeRepository.findByName(name);
    }
    public Optional<CityCode> findByCityCodeAndAreaCodeId(String cityCode, Long areaCodeId) {
        return cityCodeRepository.findByCityCodeAndAreaCodeId(cityCode, areaCodeId);
    }

    public Optional<AreaCode> findAreaCodeByCityCodeAndAreaCodeId(String cityCode, Long areaCodeId) {
        return cityCodeRepository.findAreaCodeByCityCodeAndAreaCodeId(cityCode, areaCodeId);
    }

    public List<CityCode> findAll() {
        return cityCodeRepository.findAll();
    }
}
