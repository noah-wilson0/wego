package com.wego.wego.domain.chemi.repository;

import com.wego.wego.domain.chemi.entity.Chemi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChemiRepository extends JpaRepository<Chemi, Long> {

    Optional<Chemi> findByName(String name);

}
