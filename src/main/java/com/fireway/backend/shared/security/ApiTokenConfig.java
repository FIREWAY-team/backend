package com.fireway.backend.shared.security;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiTokenConfig implements WebMvcConfigurer {
    /**
     * 토큰이 필요한 경로.
     *
     * 신고는 주소·좌표·현장 사진이 붙는 유일한 도메인이고, 업로드 URL 발급은 누구나
     * 50MB 짜리 presigned URL 을 받아갈 수 있는 자리다. 나머지(지도·차량·시나리오·
     * 경로 계산)는 공개 데모 데이터라 열어 둔다 — 막으면 시연 화면이 먼저 죽는다.
     */
    public static final List<String> PROTECTED =
            List.of("/api/incidents", "/api/incidents/**", "/api/files/upload-url");

    private final ApiTokenInterceptor interceptor;
    public ApiTokenConfig(ApiTokenInterceptor interceptor) { this.interceptor = interceptor; }

    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns(PROTECTED);
    }
}
