package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.services.chatbot.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatbot")
@ApiResponse(responseCode = "401", description = "Authentication required")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @Operation(summary = "List chat sessions")
    @GetMapping("/sessions")
    public PageResponse<DomainResponses.ChatSessionResponse> listSessions(@PageableDefault(size = 20) Pageable pageable) {
        return chatbotService.listSessions(pageable);
    }

    @Operation(summary = "Create chat session")
    @PostMapping("/sessions")
    public DomainResponses.ChatSessionResponse createSession() {
        return chatbotService.createSession();
    }

    @Operation(summary = "Get chat session")
    @GetMapping("/sessions/{id}")
    public DomainResponses.ChatSessionResponse getSession(@PathVariable UUID id) {
        return chatbotService.getSession(id);
    }

    @Operation(summary = "Send message to chat session")
    @PostMapping("/sessions/{id}/messages")
    public DomainResponses.ChatSessionResponse sendMessage(@PathVariable UUID id,
                                                           @Valid @RequestBody DomainRequests.ChatMessageRequest request) {
        return chatbotService.sendMessage(id, request);
    }

    @Operation(summary = "Delete chat session")
    @DeleteMapping("/sessions/{id}")
    public AuthResponses.MessageResponse deleteSession(@PathVariable UUID id) {
        return chatbotService.deleteSession(id);
    }
}
