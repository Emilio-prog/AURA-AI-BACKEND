package com.auraia.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.auraia.backend.config.AppProperties;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AiChatRateLimitFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chatMessagesAreRateLimitedPerAuthenticatedUser() throws Exception {
        AppProperties properties = new AppProperties();
        properties.getAi().setChatRateLimitCapacity(1);
        properties.getAi().setChatRateLimitRefillMinutes(5);
        AiChatRateLimitFilter filter = new AiChatRateLimitFilter(properties);
        UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(),
            "demo@aura.ai",
            "hash",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        AtomicInteger calls = new AtomicInteger();

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(request(), firstResponse, (request, response) -> calls.incrementAndGet());
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(request(), secondResponse, (request, response) -> calls.incrementAndGet());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentAsString()).contains("Too many chat messages");
        assertThat(calls).hasValue(1);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "POST",
            "/api/v1/chatbot/sessions/11111111-1111-1111-1111-111111111111/messages"
        );
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
