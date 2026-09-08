package team.fireway.goldenlane.shared.exception;
import org.junit.jupiter.api.Test; import org.springframework.mock.web.*; import org.springframework.http.*; import static org.junit.jupiter.api.Assertions.*;
class GlobalExceptionHandlerTest { @Test void notFoundMapsTo404(){var h=new GlobalExceptionHandler(); var r=h.domain(new NotFoundException("없음")); assertEquals(HttpStatus.NOT_FOUND,r.getStatusCode());} }

