package com.fireway.backend.shared.exception;
public final class UnauthorizedException extends DomainException { public UnauthorizedException(String m) { super(m, "UNAUTHORIZED"); } public int httpStatus() { return 401; } }
