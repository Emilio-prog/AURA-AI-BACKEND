package com.auraia.backend.services.chatbot;

import com.auraia.backend.clients.AiAnalyzerClient;
import com.auraia.backend.clients.dto.AiChatResponse;
import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.entities.ChatSession;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.ChatSessionRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final AiAnalyzerClient aiAnalyzerClient;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DomainResponses.ChatSessionResponse> listSessions(Pageable pageable) {
        return PageResponse.from(chatSessionRepository.findByUser(currentUser(), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public DomainResponses.ChatSessionResponse createSession() {
        ChatSession session = ChatSession.builder()
            .user(currentUser())
            .title("Nueva sesion")
            .startedAt(Instant.now())
            .build();
        return toResponse(chatSessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public DomainResponses.ChatSessionResponse getSession(UUID id) {
        return toResponse(findOwned(id));
    }

    @Override
    @Transactional
    public DomainResponses.ChatSessionResponse sendMessage(UUID id, DomainRequests.ChatMessageRequest request) {
        ChatSession session = findOwned(id);
        List<Map<String, Object>> messages = new ArrayList<>(session.getMessages());
        messages.add(message("user", request.message(), Map.of()));
        AiChatResponse aiResponse = aiAnalyzerClient.chat(messages, request.message());
        messages.add(message("assistant", aiResponse.reply(), Map.of(
            "sentiment", aiResponse.sentiment(),
            "riskLevel", aiResponse.riskLevel(),
            "emotions", aiResponse.emotions()
        )));
        if ("Nueva sesion".equals(session.getTitle())) {
            session.setTitle(request.message().length() > 60 ? request.message().substring(0, 60) : request.message());
        }
        session.setMessages(messages);
        return toResponse(chatSessionRepository.save(session));
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse deleteSession(UUID id) {
        chatSessionRepository.delete(findOwned(id));
        return new AuthResponses.MessageResponse("OK");
    }

    private ChatSession findOwned(UUID id) {
        return chatSessionRepository.findByIdAndUser(id, currentUser())
            .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
    }

    private Map<String, Object> message(String role, String content, Map<String, Object> metadata) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        message.put("timestamp", Instant.now().toString());
        message.putAll(metadata);
        return message;
    }

    private DomainResponses.ChatSessionResponse toResponse(ChatSession session) {
        return new DomainResponses.ChatSessionResponse(
            session.getId(),
            session.getTitle(),
            session.getMessages(),
            session.getStartedAt(),
            session.getUpdatedAt()
        );
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
