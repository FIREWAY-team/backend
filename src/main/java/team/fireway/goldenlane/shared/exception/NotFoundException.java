package team.fireway.goldenlane.shared.exception;
import org.springframework.http.HttpStatus;
public final class NotFoundException extends DomainException {
 public NotFoundException(String m){super(ErrorCode.NOT_FOUND,m);} public NotFoundException(ErrorCode c,String m){super(c,m);}
 public HttpStatus httpStatus(){return HttpStatus.NOT_FOUND;}
}

