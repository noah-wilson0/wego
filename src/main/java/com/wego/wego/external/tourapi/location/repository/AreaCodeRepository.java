package com.wego.wego.external.tourapi.location.repository;

import com.wego.wego.external.tourapi.location.entity.AreaCode;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AreaCodeRepository extends JpaRepository<AreaCode,Long> {
    Optional<AreaCode> findByName(String name);
    Optional<AreaCode> findByAreaCode(String areaCode);

    @Query(value = "SELECT * FROM area_code WHERE POSITION(name IN :name) > 0", nativeQuery = true)
    Optional<AreaCode> findByNameContainedIn(@Param("input") String name);


}
