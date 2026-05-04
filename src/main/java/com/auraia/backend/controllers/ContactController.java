package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.services.contact.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/contacts")
@ApiResponse(responseCode = "401", description = "Authentication required")
public class ContactController {

    private final ContactService contactService;

    @Operation(summary = "List trusted contacts")
    @GetMapping
    public List<DomainResponses.ContactResponse> list() {
        return contactService.list();
    }

    @Operation(summary = "Create trusted contact")
    @PostMapping
    public DomainResponses.ContactResponse create(@Valid @RequestBody DomainRequests.ContactRequest request) {
        return contactService.create(request);
    }

    @Operation(summary = "Update trusted contact")
    @PutMapping("/{id}")
    public DomainResponses.ContactResponse update(@PathVariable UUID id,
                                                  @Valid @RequestBody DomainRequests.ContactRequest request) {
        return contactService.update(id, request);
    }

    @Operation(summary = "Delete trusted contact")
    @DeleteMapping("/{id}")
    public AuthResponses.MessageResponse delete(@PathVariable UUID id) {
        return contactService.delete(id);
    }
}
