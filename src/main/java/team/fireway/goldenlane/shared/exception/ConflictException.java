package team.fireway.goldenlane.shared.exception;
import org.springframework.http.HttpStatus;
public final class ConflictException extends DomainException { public ConflictException(String m){super(ErrorCode.CONFLICT,m);} public HttpStatus httpStatus(){return HttpStatus.CONFLICT;} }

