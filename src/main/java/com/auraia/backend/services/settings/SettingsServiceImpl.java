package com.auraia.backend.services.settings;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.mappers.UserSettingsMapper;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.Theme;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.security.SecurityUtils;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private final UserSettingsMapper userSettingsMapper;

    @Override
    @Transactional
    public DomainResponses.UserSettingsResponse get() {
        return userSettingsMapper.toResponse(findOrCreate(currentUser()));
    }

    @Override
    @Transactional
    public DomainResponses.UserSettingsResponse update(DomainRequests.UserSettingsRequest request) {
        UserSettings settings = findOrCreate(currentUser());
        if (request.theme() != null) {
            settings.setTheme(request.theme());
        }
        if (request.language() != null && !request.language().isBlank()) {
            settings.setLanguage(request.language().trim());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            settings.setTimezone(request.timezone().trim());
        }
        settings.setNotificationPreferences(request.notificationPreferences() == null
            ? new LinkedHashMap<>()
            : request.notificationPreferences());
        return userSettingsMapper.toResponse(userSettingsRepository.save(settings));
    }

    private UserSettings findOrCreate(User user) {
        return userSettingsRepository.findByUser(user).orElseGet(() -> userSettingsRepository.save(UserSettings.builder()
            .user(user)
            .theme(Theme.SYSTEM)
            .language("es")
            .timezone("Europe/Madrid")
            .build()));
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
