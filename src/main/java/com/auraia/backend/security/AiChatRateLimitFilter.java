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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AiChatRateLimitFilter extends OncePerRequestFilter {

    private final AppProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AiChatRateLimitFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isChatMessageEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = bucketKey(request);
        if (key == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many chat messages\"}");
    }

    private boolean isChatMessageEndpoint(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return request.getRequestURI().matches(".*/api/v1/chatbot/sessions/[^/]+/messages$");
    }

    private String bucketKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.id() + ":chat";
        }
        return null;
    }

    private Bucket newBucket() {
        long capacity = properties.getAi().getChatRateLimitCapacity();
        Duration refillPeriod = Duration.ofMinutes(properties.getAi().getChatRateLimitRefillMinutes());
        return Bucket.builder()
            .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, refillPeriod))
            .build();
    }
}
