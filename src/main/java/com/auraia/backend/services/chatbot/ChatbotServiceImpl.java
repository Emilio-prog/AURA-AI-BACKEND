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
import com.auraia.backend.services.privacy.ContentCryptoService;
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
    private final ContentCryptoService contentCryptoService;

    @Override
    @Transactional
    public PageResponse<DomainResponses.ChatSessionResponse> listSessions(Pageable pageable) {
        User user = currentUser();
        return PageResponse.from(chatSessionRepository.findByUser(user, pageable).map(session -> toResponse(session, user, true)));
    }

    @Override
    @Transactional
    public DomainResponses.ChatSessionResponse createSession() {
        User user = currentUser();
        ChatSession session = ChatSession.builder()
            .user(user)
            .title(contentCryptoService.encrypt(user.getId(), "chat.title", "Nueva sesion"))
            .startedAt(Instant.now())
            .build();
        return toResponse(chatSessionRepository.save(session), user, false);
    }

    @Override
    @Transactional
    public DomainResponses.ChatSessionResponse getSession(UUID id) {
        User user = currentUser();
        return toResponse(findOwned(id, user), user, true);
    }

    @Override
    @Transactional
    public DomainResponses.ChatSessionResponse sendMessage(UUID id, DomainRequests.ChatMessageRequest request) {
        User user = currentUser();
        ChatSession session = findOwned(id, user);
        List<Map<String, Object>> messages = decryptedMessages(user.getId(), session.getMessages());
        messages.add(message("user", request.message(), Map.of()));
        AiChatResponse aiResponse = aiAnalyzerClient.chat(messages, request.message());
        messages.add(message("assistant", aiResponse.reply(), Map.of(
            "sentiment", aiResponse.sentiment(),
            "riskLevel", aiResponse.riskLevel(),
            "emotions", aiResponse.emotions()
        )));
        String title = contentCryptoService.decrypt(user.getId(), "chat.title", session.getTitle());
        if ("Nueva sesion".equals(title)) {
            title = request.message().length() > 60 ? request.message().substring(0, 60) : request.message();
        }
        session.setTitle(contentCryptoService.encrypt(user.getId(), "chat.title", title));
        session.setMessages(encryptedMessages(user.getId(), messages));
        ChatSession saved = chatSessionRepository.save(session);
        return new DomainResponses.ChatSessionResponse(saved.getId(), title, messages, saved.getStartedAt(), saved.getUpdatedAt());
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse deleteSession(UUID id) {
        chatSessionRepository.delete(findOwned(id, currentUser()));
        return new AuthResponses.MessageResponse("OK");
    }

    private ChatSession findOwned(UUID id) {
        return findOwned(id, currentUser());
    }

    private ChatSession findOwned(UUID id, User user) {
        return chatSessionRepository.findByIdAndUser(id, user)
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

    private DomainResponses.ChatSessionResponse toResponse(ChatSession session, User user, boolean protectLegacy) {
        String title = contentCryptoService.decrypt(user.getId(), "chat.title", session.getTitle());
        List<Map<String, Object>> messages = decryptedMessages(user.getId(), session.getMessages());
        if (protectLegacy && contentCryptoService.isEnabled()) {
            boolean titleNeedsProtection = session.getTitle() != null && !contentCryptoService.isEncrypted(session.getTitle());
            boolean messagesNeedProtection = hasLegacyMessageContent(session.getMessages());
            if (titleNeedsProtection || messagesNeedProtection) {
                if (titleNeedsProtection) {
                    session.setTitle(contentCryptoService.encrypt(user.getId(), "chat.title", title));
                }
                if (messagesNeedProtection) {
                    session.setMessages(encryptedMessages(user.getId(), messages));
                }
                chatSessionRepository.save(session);
            }
        }
        return new DomainResponses.ChatSessionResponse(
            session.getId(),
            title,
            messages,
            session.getStartedAt(),
            session.getUpdatedAt()
        );
    }

    private List<Map<String, Object>> decryptedMessages(UUID userId, List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>(messages.size());
        for (Map<String, Object> source : messages) {
            Map<String, Object> copy = new LinkedHashMap<>(source);
            Object content = copy.get("content");
            if (content instanceof String text) {
                copy.put("content", contentCryptoService.decrypt(userId, "chat.message-content", text));
            }
            result.add(copy);
        }
        return result;
    }

    private List<Map<String, Object>> encryptedMessages(UUID userId, List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>(messages.size());
        for (Map<String, Object> source : messages) {
            Map<String, Object> copy = new LinkedHashMap<>(source);
            Object content = copy.get("content");
            if (content instanceof String text) {
                copy.put("content", contentCryptoService.encrypt(userId, "chat.message-content", text));
            }
            result.add(copy);
        }
        return result;
    }

    private boolean hasLegacyMessageContent(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        for (Map<String, Object> message : messages) {
            Object content = message.get("content");
            if (content instanceof String text && !contentCryptoService.isEncrypted(text)) {
                return true;
            }
        }
        return false;
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
