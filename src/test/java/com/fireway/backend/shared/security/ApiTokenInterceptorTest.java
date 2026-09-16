package com.fireway.backend.shared.security;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fireway.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiTokenInterceptorTest {
    /** 보호 대상 경로를 흉내내는 최소 컨트롤러. 인터셉터 계약만 본다. */
    @RestController @RequestMapping("/api/incidents")
    static class Stub {
        @GetMapping public String list() { return "ok"; }
    }

    private static MockMvc mvcWith(String token) {
        return MockMvcBuilders.standaloneSetup(new Stub())
                .addInterceptors(new ApiTokenInterceptor(token))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 프론트 BFF 가 헤더를 붙이기 전에 백엔드만 배포돼도 사이트가 살아 있어야 한다.
    @Test void 토큰이_비어_있으면_그냥_통과시킨다() throws Exception {
        mvcWith("").perform(get("/api/incidents")).andExpect(status().isOk());
        mvcWith("   ").perform(get("/api/incidents")).andExpect(status().isOk());
    }

    @Test void 토큰이_켜져_있으면_헤더_없이는_401() throws Exception {
        mvcWith("s3cret").perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test void 틀린_토큰도_401() throws Exception {
        mvcWith("s3cret").perform(get("/api/incidents").header(ApiTokenInterceptor.HEADER, "wrong"))
                .andExpect(status().isUnauthorized());
    }

    // 토큰을 한 바이트씩 맞혀 나가는 것을 막으려고 길이가 다른 값도 같은 경로로 떨어져야 한다.
    @Test void 앞부분만_맞는_토큰도_401() throws Exception {
        mvcWith("s3cret").perform(get("/api/incidents").header(ApiTokenInterceptor.HEADER, "s3c"))
                .andExpect(status().isUnauthorized());
    }

    @Test void 맞는_토큰이면_통과한다() throws Exception {
        mvcWith("s3cret").perform(get("/api/incidents").header(ApiTokenInterceptor.HEADER, "s3cret"))
                .andExpect(status().isOk());
    }
}
