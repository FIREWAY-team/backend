package com.fireway.backend.shared.exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(DomainException.class) ResponseEntity<ErrorResponse> domain(DomainException e, HttpServletRequest r) { return response(e.httpStatus(), e.getCode(), e.getMessage(), r); }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class}) ResponseEntity<ErrorResponse> validation(Exception e, HttpServletRequest r) { return response(422, "VALIDATION_ERROR", "요청 값이 유효하지 않습니다.", r); }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<ErrorResponse> malformed(Exception e, HttpServletRequest r) { return response(400, "MALFORMED_REQUEST", "요청 본문을 읽을 수 없습니다.", r); }
    @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) { log.error("Unhandled exception", e); return response(500, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", r); }
    private ResponseEntity<ErrorResponse> response(int status, String code, String message, HttpServletRequest r) { return ResponseEntity.status(status).body(new ErrorResponse(new ErrorResponse.ErrorBody(code, message, r.getHeader("X-Request-Id")))); }
}
