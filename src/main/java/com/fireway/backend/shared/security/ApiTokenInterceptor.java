package com.fireway.backend.shared.security;
import com.fireway.backend.shared.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 신고 도메인과 업로드 URL 발급을 공유 토큰으로 막는다.
 *
 * 필터가 아니라 인터셉터인 이유: 여기서 던진 UnauthorizedException 이
 * GlobalExceptionHandler 를 그대로 타서 다른 API 와 같은 오류 형식으로 나간다.
 *
 * 토큰이 비어 있으면 통과시킨다. 프론트 BFF 가 헤더를 붙이기 전에 백엔드만 먼저
 * 배포되면 사이트가 통째로 죽기 때문이다. 양쪽 환경변수에 API_TOKEN 을 넣는 순간
 * 켜지고, 그 전까지는 기동할 때마다 경고를 남긴다.
 */
@Component
public class ApiTokenInterceptor implements HandlerInterceptor {
    public static final String HEADER = "X-Api-Token";
    private static final Logger log = LoggerFactory.getLogger(ApiTokenInterceptor.class);

    private final byte[] expected;

    public ApiTokenInterceptor(@Value("${API_TOKEN:}") String token) {
        String trimmed = token == null ? "" : token.trim();
        this.expected = trimmed.getBytes(StandardCharsets.UTF_8);
        if (trimmed.isEmpty()) {
            log.warn("API_TOKEN 이 비어 있습니다. {} 가 인증 없이 열려 있습니다.", ApiTokenConfig.PROTECTED);
        }
    }

    public boolean enabled() { return expected.length > 0; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled() || CorsUtils.isPreFlightRequest(request)) return true;
        String given = request.getHeader(HEADER);
        if (given == null) throw new UnauthorizedException(HEADER + " 헤더가 필요합니다.");
        // 길이까지 비교해 주는 상수 시간 비교. 토큰을 한 바이트씩 맞혀 나가는 것을 막는다.
        if (!MessageDigest.isEqual(given.trim().getBytes(StandardCharsets.UTF_8), expected)) {
            throw new UnauthorizedException(HEADER + " 값이 올바르지 않습니다.");
        }
        return true;
    }
}
