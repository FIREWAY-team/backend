package com.fireway.backend.shared.exception;
public final class InfrastructureException extends DomainException { public InfrastructureException(String m) { super(m, "INFRASTRUCTURE_ERROR"); } public int httpStatus() { return 500; } }
