package com.wego.wego.external.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@Getter
@ToString
public class RouteResult {

    private String originId;
    private String destinationId;
    private int fare;
    private int distance; //전체 검색 결과 거리(미터)
    private int duration; //목적지까지 소요 시간(초)

    public void changeOrigin(String originId) {
        this.originId = originId;
    }
    public void changeDestination(String destinationId) {
        this.destinationId = destinationId;
    }
    public void changeTaxiFare(int fare) {
        this.fare = fare;
    }
    public void changeDistance(int distance) {
        this.distance = distance;
    }
    public void changeDuration(int duration) {
        this.duration = duration;
    }
    public void changeAll() {
        this.originId = originId;
        this.destinationId = destinationId;
        this.fare = fare;
        this.distance = distance;
        this.duration = duration;
    }
}
