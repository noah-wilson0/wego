package com.wego.wego.external.tourapi.place.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
@Slf4j
@SpringBootTest
class PlaceFetchServiceTest {

    @Autowired
    private PlaceFetchService placeFetchService;

    @Autowired
    private PlaceService placeService;
    @Test
    void fetchPlace() {
        placeFetchService.fetchPlace();
    }
    @Transactional(readOnly = true)
    @Test
    void printPlace() {
        log.info(placeService.findById(1L).get().toString());
    }
}