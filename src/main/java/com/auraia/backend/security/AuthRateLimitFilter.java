package com.auraia.backend.security;

import com.auraia.backend.config.AppProperties;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final AppProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isRateLimitedAuthEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many authentication attempts\"}");
    }

    private boolean isRateLimitedAuthEndpoint(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.endsWith("/api/v1/auth/login")
            || path.endsWith("/api/v1/auth/register")
            || path.endsWith("/api/v1/auth/forgot-password")
            || path.endsWith("/api/v1/auth/resend-verification");
    }

    private Bucket newBucket() {
        long capacity = properties.getRateLimit().getAuthCapacity();
        Duration refillPeriod = Duration.ofMinutes(properties.getRateLimit().getAuthRefillMinutes());
        return Bucket.builder()
            .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, refillPeriod))
            .build();
    }
}
