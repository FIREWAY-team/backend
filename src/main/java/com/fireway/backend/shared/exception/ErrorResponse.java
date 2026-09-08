package com.fireway.backend.shared.exception;
public record ErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, String requestId) { }
}
