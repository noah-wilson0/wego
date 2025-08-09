package com.wego.wego.global.enums;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class PlaceTypeTest {

    @Test
    void test() {
        log.info(PlaceType.ACCOMMODATION.getCode());
    }

}