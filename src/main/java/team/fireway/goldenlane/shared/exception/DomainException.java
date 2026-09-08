package team.fireway.goldenlane.shared.exception;
import org.springframework.http.HttpStatus;
public sealed abstract class DomainException extends RuntimeException permits NotFoundException,ValidationException,ConflictException,UnauthorizedException,ExternalSystemException,InfrastructureException {
 private final ErrorCode code;
 protected DomainException(ErrorCode code,String message){super(message);this.code=code;}
 public ErrorCode code(){return code;} public abstract HttpStatus httpStatus();
}

