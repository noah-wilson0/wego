package com.wego.wego.external.tourapi.location.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AreaCodeFetchServiceTest {

    @Autowired
    private AreaCodeFetchService areaCodeFetchService;

    @Test
    void fetchAreaCode() {
        areaCodeFetchService.fetchAreaCode();
    }
}