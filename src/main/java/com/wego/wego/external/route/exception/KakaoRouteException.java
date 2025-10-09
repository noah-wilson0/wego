package com.wego.wego.external.route.exception;

public class KakaoRouteException extends RuntimeException{
    private final int code;
    private final String originId;
    private final String destinationId;
    public KakaoRouteException(int code, String message, String originId, String destinationId) {
        super("Kakao route failed (code=" + code + ", msg=" + message + ")");
        this.code = code;
        this.originId = originId;
        this.destinationId = destinationId;
    }
    public int getCode() { return code; }
    public String getOriginId() { return originId; }
    public String getDestinationId() { return destinationId; }
}
