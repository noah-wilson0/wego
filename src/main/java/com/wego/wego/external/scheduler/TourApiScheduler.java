package com.wego.wego.external.scheduler;

import com.wego.wego.external.tourapi.location.service.AreaCodeFetchService;
import com.wego.wego.external.tourapi.place.service.PlaceFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiScheduler {

    private final PlaceFetchService placeFetchService;
    @Scheduled(cron = "0 0 0 * * *")
    public void syncAreaCodes() {
        log.info("[스케줄러] 지역 코드 동기화 시작");
        placeFetchService.fetchPlace();
        log.info("[스케줄러] 지역 코드 동기화 완료");
    }
}