package com.auraia.backend.services.achievement;

import com.auraia.backend.models.dto.request.AchievementRequests;
import com.auraia.backend.models.dto.response.AchievementResponses;

public interface AchievementService {

    AchievementResponses.AchievementListResponse list();

    AchievementResponses.AchievementListResponse recordEvent(AchievementRequests.EventRequest request);
}
