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

@WebMvcTest(controllers = GlobalExceptionHandlerTest.Probe.class)
@Import(GlobalExceptionHandler.class)
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
