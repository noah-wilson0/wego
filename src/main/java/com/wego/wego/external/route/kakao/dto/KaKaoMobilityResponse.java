package com.wego.wego.external.route.kakao.dto;

import java.util.List;

/**
 * 카카오 모빌리티 길찾기 Directions API 응답 객체.
 * 참고: https://developers.kakaomobility.com/docs/navi-api/directions/
 *
 */
public record KaKaoMobilityResponse(
        String trans_id,
        List<Route> routes
) {
    public record Route(
            int result_code,
            String result_msg,
            Summary summary
    ) {}

    public record Summary(
            Origin origin,
            Destination destination,
            List<Waypoints> waypoints,
            String priority,
            Fare fare,
            int distance,
            int duration
    ) {}

    public record Origin(Coordinate coordinate) {}

    public record Destination(Coordinate coordinate) {}

    public record Waypoints(Coordinate coordinate) {}

    public record Coordinate(
            String name,
            double x,
            double y
    ) {}

    public record Fare(int taxi) {}
}



