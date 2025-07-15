package com.wego.wego.external.tourapi.place.repository;

import com.wego.wego.external.tourapi.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Place findByTitle(String title);
    List<Place> findAllByContentIdIn(List<String> contentIds);
    Optional<Place> findByContentId(String contentId);
}

