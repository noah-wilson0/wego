package com.wego.wego.external.route.tmap.dto;

public record TMapTransitRequest(
        String startX,
        String startY,
        String endX,
        String endY,
//        int lang,
//        String format,
        int count
) {

}
