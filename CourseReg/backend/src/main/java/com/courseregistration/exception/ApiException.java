package com.courseregistration.exception;

/**
 * A deliberate, expected error — "course is full", "username taken",
 * "not your enrollment", etc. — that should be surfaced to the client
 * as a clean HTTP status + message instead of a generic 500.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
