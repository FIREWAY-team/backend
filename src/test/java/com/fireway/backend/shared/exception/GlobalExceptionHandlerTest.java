package com.fireway.backend.shared.exception;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;

// Probe 는 @Import 로도 등록해야 한다. @WebMvcTest(controllers=) 는 스캔 필터일 뿐이라
// 테스트 클래스 안의 static 컨트롤러는 빈으로 올라오지 않는다. 이게 빠져 있던 동안 /probe/* 가
// 전부 "매핑 없는 경로"로 빠져 404 만 돌아왔고, notFoundIs404 는 그 404 를 보고 통과하고 있었다.
@WebMvcTest(controllers = GlobalExceptionHandlerTest.Probe.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.Probe.class})
class GlobalExceptionHandlerTest {
    @org.springframework.beans.factory.annotation.Autowired MockMvc mvc;
    @Test void notFoundIs404() throws Exception { mvc.perform(get("/probe/404")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND")); }
    @Test void validationIs422() throws Exception { mvc.perform(get("/probe/422")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR")); }
    @Test void unmappedPathIs404() throws Exception { mvc.perform(get("/이런경로는없다")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND")); }
    @Test void wrongMethodIs405() throws Exception { mvc.perform(post("/probe/404")).andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED")); }
    @Test void unexpectedIs500() throws Exception { mvc.perform(get("/probe/500")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR")); }
    @RestController @RequestMapping("/probe") static class Probe {
        @GetMapping("/404") String notFound() { throw new NotFoundException("missing"); }
        @GetMapping("/422") String validation() { throw new ValidationException("bad"); }
        @GetMapping("/500") String error() { throw new IllegalStateException("boom"); }
    }
}
