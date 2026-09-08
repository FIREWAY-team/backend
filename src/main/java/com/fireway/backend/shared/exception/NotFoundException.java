package com.fireway.backend.shared.exception;
public final class NotFoundException extends DomainException { public NotFoundException(String m) { super(m, "NOT_FOUND"); } public int httpStatus() { return 404; } }
