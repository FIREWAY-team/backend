package com.fireway.backend.shared.exception;
public sealed abstract class DomainException extends RuntimeException
        permits NotFoundException, ValidationException, ConflictException, UnauthorizedException, ExternalSystemException, InfrastructureException {
    private final String code;
    protected DomainException(String message, String code) { super(message); this.code = code; }
    public String getCode() { return code; }
    public abstract int httpStatus();
}
