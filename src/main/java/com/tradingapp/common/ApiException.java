package com.tradingapp.common;

import org.springframework.http.HttpStatus;

/** Base class for exceptions that carry the HTTP status the client should see. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
