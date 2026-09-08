package team.fireway.goldenlane.shared.exception;
import org.springframework.http.HttpStatus;
public final class ValidationException extends DomainException { public ValidationException(String m){super(ErrorCode.VALIDATION_FAILED,m);} public HttpStatus httpStatus(){return HttpStatus.BAD_REQUEST;} }

