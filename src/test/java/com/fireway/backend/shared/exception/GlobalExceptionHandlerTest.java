package com.fireway.backend.shared.exception;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;

// Probe 는 테스트 안 중첩 클래스라 컴포넌트 스캔에 걸리지 않는다. controllers 로 지정만 하면
// 매핑이 등록되지 않아서 요청이 정적 리소스 핸들러로 떨어지고 전부 500이 된다. @Import 로 직접 등록한다.
@WebMvcTest(controllers = GlobalExceptionHandlerTest.Probe.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.Probe.class})
class GlobalExceptionHandlerTest {
    @org.springframework.beans.factory.annotation.Autowired MockMvc mvc;
    @Test void notFoundIs404() throws Exception { mvc.perform(get("/probe/404")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND")); }
    @Test void validationIs422() throws Exception { mvc.perform(get("/probe/422")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR")); }
    @Test void unexpectedIs500() throws Exception { mvc.perform(get("/probe/500")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR")); }
    @RestController @RequestMapping("/probe") static class Probe {
        @GetMapping("/404") String notFound() { throw new NotFoundException("missing"); }
        @GetMapping("/422") String validation() { throw new ValidationException("bad"); }
        @GetMapping("/500") String error() { throw new IllegalStateException("boom"); }
    }
}
