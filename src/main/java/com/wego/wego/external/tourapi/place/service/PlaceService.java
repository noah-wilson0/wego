package com.wego.wego.external.tourapi.place.service;


import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;

    public Place save(Place place) {
        return placeRepository.save(place);
    }


    public Optional<Place> findById(Long id) {
        return placeRepository.findById(id);
    }

    public List<Place> findByTitleContaining(String title) {
        return placeRepository.findByTitleContaining(title);
    }
    public List<Place> findByFullAddress(String fullAddr) {
        return placeRepository.findByFullAddress(fullAddr);
    }

    public Optional<Place> findByContentId(String contentId) {
        return placeRepository.findByContentId(contentId);
    }

    public Place findByTitle(String title) {
        return placeRepository.findByTitle(title);
    }

    public Optional<Place> findMostSimilarTitleInCity(String title, Long cityCodeId) {
        return placeRepository.findMostSimilarTitleInCity(title, cityCodeId);
    }

    public List<Place> findAll() {
        return placeRepository.findAll();
    }

    public List<Place> findAllByContentIdIn(List<String> contentIds) {
        return placeRepository.findAllByContentIdIn(contentIds);
    }

}
