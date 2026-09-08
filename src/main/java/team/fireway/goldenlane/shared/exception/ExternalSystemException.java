package team.fireway.goldenlane.shared.exception;
import org.springframework.http.HttpStatus;
public final class ExternalSystemException extends DomainException { public ExternalSystemException(String m){super(ErrorCode.EXTERNAL_SYSTEM_ERROR,m);} public HttpStatus httpStatus(){return HttpStatus.BAD_GATEWAY;} }

