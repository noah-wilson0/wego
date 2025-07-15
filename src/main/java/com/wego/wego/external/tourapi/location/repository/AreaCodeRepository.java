package com.wego.wego.external.tourapi.location.repository;

import com.wego.wego.external.tourapi.location.entity.AreaCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AreaCodeRepository extends JpaRepository<AreaCode,Long> {
    Optional<AreaCode> findByName(String name);
    Optional<AreaCode> findByAreaCode(String areaCode);
}
