package com.fireway.backend.shared.exception;
public final class ConflictException extends DomainException { public ConflictException(String m) { super(m, "CONFLICT"); } public int httpStatus() { return 409; } }
