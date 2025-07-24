package com.wego.wego.external.tourapi.location.service;

import com.wego.wego.external.tourapi.location.entity.AreaCode;
import com.wego.wego.external.tourapi.location.repository.AreaCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.geom.Area;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AreaCodeService {

    private final AreaCodeRepository areaCodeRepository;

    public AreaCode save(AreaCode areaCode) {
        return areaCodeRepository.save(areaCode);
    }

    public Optional<AreaCode> findByAreaCode(String areaCode) {
        return areaCodeRepository.findByAreaCode(areaCode);
    }

    public Optional<AreaCode> findByName(String name) {
        return areaCodeRepository.findByName(name);
    }

    public Optional<AreaCode> findByNameContainedIn(String name) {
        return areaCodeRepository.findByNameContainedIn(name);
    }

    public List<AreaCode> findAll() {
        return areaCodeRepository.findAll();
    }

}