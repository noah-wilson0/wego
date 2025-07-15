package com.wego.wego.external.tourapi.location.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
@Transactional
@SpringBootTest
class CityCodeFetchServiceTest {

    @Autowired
    private CityCodeFetchService cityCodeFetchService;
    @Test
    void fetchCityCodes() {
        cityCodeFetchService.fetchCityCodes();
    }
}