package com.auraia.backend.services.admin;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.mappers.UserMapper;
import com.auraia.backend.models.dto.request.AdminRequests;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
import com.auraia.backend.services.AuditService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponses.AdminUserResponse> listUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toAdminResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponses.AdminUserResponse getUser(UUID id) {
        return userMapper.toAdminResponse(findUser(id));
    }

    @Override
    @Transactional
    public UserResponses.AdminUserResponse updateUser(UUID id, AdminRequests.AdminUpdateUserRequest request) {
        User target = findUser(id);
        if (request.role() != null) {
            target.setRole(request.role());
        }
        if (request.plan() != null) {
            target.setPlan(request.plan());
        }
        if (request.emailVerified() != null) {
            target.setEmailVerified(request.emailVerified());
        }
        User saved = userRepository.save(target);
        audit("ADMIN_UPDATE_USER", saved, Map.of("role", saved.getRole().name(), "plan", saved.getPlan().name()));
        return userMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public UserResponses.AdminUserResponse softDeleteUser(UUID id) {
        User target = findUser(id);
        target.setDeletedAt(Instant.now());
        User saved = userRepository.save(target);
        audit("ADMIN_SOFT_DELETE_USER", saved, Map.of());
        return userMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public UserResponses.AdminUserResponse restoreUser(UUID id) {
        User target = findUser(id);
        target.setDeletedAt(null);
        User saved = userRepository.save(target);
        audit("ADMIN_RESTORE_USER", saved, Map.of());
        return userMapper.toAdminResponse(saved);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void audit(String action, User target, Map<String, Object> metadata) {
        User actor = userRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        auditService.record(actor, action, "User", target.getId().toString(), metadata);
    }
}
