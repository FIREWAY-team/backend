package team.fireway.goldenlane.shared.exception;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.MDC;
@RestControllerAdvice
public class GlobalExceptionHandler {
 private static final Logger log=LoggerFactory.getLogger(GlobalExceptionHandler.class);
 private String requestId(){return MDC.get("requestId");}
 @ExceptionHandler(DomainException.class) public ResponseEntity<ErrorResponse> domain(DomainException e){return ResponseEntity.status(e.httpStatus()).body(new ErrorResponse(new ErrorResponse.ErrorBody(e.code().name(),e.getMessage(),requestId())));}
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e){return ResponseEntity.unprocessableEntity().body(body(ErrorCode.VALIDATION_FAILED.name(),"요청 값이 유효하지 않습니다."));}
 @ExceptionHandler(HttpMessageNotReadableException.class) public ResponseEntity<ErrorResponse> unreadable(){return ResponseEntity.badRequest().body(body(ErrorCode.INVALID_REQUEST.name(),"요청 본문을 읽을 수 없습니다."));}
 @ExceptionHandler(Exception.class) public ResponseEntity<ErrorResponse> fallback(Exception e){log.error("unhandled",e);return ResponseEntity.internalServerError().body(body(ErrorCode.INTERNAL_ERROR.name(),"서버 내부 오류가 발생했습니다."));}
 private ErrorResponse body(String c,String m){return new ErrorResponse(new ErrorResponse.ErrorBody(c,m,requestId()));}
}

