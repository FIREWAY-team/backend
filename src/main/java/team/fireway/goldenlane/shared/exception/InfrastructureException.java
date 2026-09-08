package team.fireway.goldenlane.shared.exception;
import org.springframework.http.HttpStatus;
public final class InfrastructureException extends DomainException { public InfrastructureException(String m){super(ErrorCode.INFRASTRUCTURE_ERROR,m);} public HttpStatus httpStatus(){return HttpStatus.INTERNAL_SERVER_ERROR;} }

