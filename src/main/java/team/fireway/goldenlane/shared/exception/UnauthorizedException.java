package team.fireway.goldenlane.shared.exception;
import org.springframework.http.HttpStatus;
public final class UnauthorizedException extends DomainException { public UnauthorizedException(String m){super(ErrorCode.UNAUTHORIZED,m);} public HttpStatus httpStatus(){return HttpStatus.UNAUTHORIZED;} }

