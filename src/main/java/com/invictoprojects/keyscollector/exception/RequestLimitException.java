package com.invictoprojects.keyscollector.exception;

public class RequestLimitException extends RuntimeException {
    private final int statusCode;

    public RequestLimitException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
