package com.wego.wego.global.exception.retry;

public class RetryRoutingException extends RuntimeException {
    public RetryRoutingException(String message) { super(message); }
    public RetryRoutingException(String message, Throwable cause) { super(message, cause); }
}

