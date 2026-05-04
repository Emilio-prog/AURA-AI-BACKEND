package com.auraia.backend.services.chatbot;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ChatbotService {

    PageResponse<DomainResponses.ChatSessionResponse> listSessions(Pageable pageable);

    DomainResponses.ChatSessionResponse createSession();

    DomainResponses.ChatSessionResponse getSession(UUID id);

    DomainResponses.ChatSessionResponse sendMessage(UUID id, DomainRequests.ChatMessageRequest request);

    AuthResponses.MessageResponse deleteSession(UUID id);
}
