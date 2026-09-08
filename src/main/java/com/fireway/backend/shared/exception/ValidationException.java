package com.fireway.backend.shared.exception;
public final class ValidationException extends DomainException { public ValidationException(String m) { super(m, "VALIDATION_ERROR"); } public int httpStatus() { return 422; } }
