package com.fireway.backend.shared.exception;
public final class ExternalSystemException extends DomainException { public ExternalSystemException(String m) { super(m, "EXTERNAL_SYSTEM_ERROR"); } public int httpStatus() { return 502; } }
