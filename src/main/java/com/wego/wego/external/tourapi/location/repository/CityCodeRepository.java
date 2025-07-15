package com.wego.wego.external.tourapi.location.repository;

import com.wego.wego.external.tourapi.location.entity.AreaCode;
import com.wego.wego.external.tourapi.location.entity.CityCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CityCodeRepository extends JpaRepository<CityCode, Long> {

    public Optional<CityCode> findByCityCode(String cityCode);
    public Optional<CityCode> findByName(String name);

    @Query("SELECT c FROM CityCode c WHERE c.cityCode = :cityCode AND c.areaCode.areaCodeId = :areaCodeId")
    Optional<CityCode> findByCityCodeAndAreaCodeId(@Param("cityCode") String cityCode, @Param("areaCodeId") Long areaCodeId);


    @Query("SELECT c.areaCode FROM CityCode c WHERE c.cityCode = :cityCode AND c.areaCode.areaCodeId = :areaCodeId")
    Optional<AreaCode> findAreaCodeByCityCodeAndAreaCodeId(@Param("cityCode") String cityCode, @Param("areaCodeId") Long areaCodeId);



}
