package com.fireway.backend.shared.logging;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

@org.springframework.stereotype.Component
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String id = request.getHeader(HEADER); if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        MDC.put("requestId", id); response.setHeader(HEADER, id);
        try { chain.doFilter(request, response); } finally { MDC.remove("requestId"); }
    }
}
