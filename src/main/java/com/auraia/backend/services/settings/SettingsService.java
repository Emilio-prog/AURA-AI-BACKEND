package com.auraia.backend.services.settings;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.DomainResponses;

public interface SettingsService {

    DomainResponses.UserSettingsResponse get();

    DomainResponses.UserSettingsResponse update(DomainRequests.UserSettingsRequest request);
}
