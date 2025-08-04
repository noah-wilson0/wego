package com.wego.wego.external.route.kakao.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Kakao Mobility 길찾기 API용 좌표 포맷터 유틸
 * 예시: "127.111202,37.394912,name=판교역"
 */
public class KaKaoCoordinateFormatter {

    public String format(String longitude, String latitude, String name) {
        return URLEncoder.encode(longitude + "," + latitude + "," + "name=" + name, StandardCharsets.UTF_8);
    }
}

