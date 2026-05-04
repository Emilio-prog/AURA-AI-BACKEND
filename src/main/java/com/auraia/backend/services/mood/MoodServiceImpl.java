package com.auraia.backend.services.mood;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.mappers.MoodLogMapper;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.entities.MoodLog;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoodServiceImpl implements MoodService {

    private final MoodLogRepository moodLogRepository;
    private final UserRepository userRepository;
    private final MoodLogMapper moodLogMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DomainResponses.MoodLogResponse> list(Instant from, Instant to, Pageable pageable) {
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now().plus(1, ChronoUnit.DAYS) : to;
        return PageResponse.from(moodLogRepository.findByUserAndLoggedAtBetween(currentUser(), start, end, pageable)
            .map(moodLogMapper::toResponse));
    }

    @Override
    @Transactional
    public DomainResponses.MoodLogResponse create(DomainRequests.MoodLogRequest request) {
        MoodLog moodLog = MoodLog.builder()
            .user(currentUser())
            .beforeLevel(request.beforeLevel())
            .afterLevel(request.afterLevel())
            .note(blankToNull(request.note()))
            .loggedAt(request.loggedAt() == null ? Instant.now() : request.loggedAt())
            .build();
        return moodLogMapper.toResponse(moodLogRepository.save(moodLog));
    }

    @Override
    @Transactional(readOnly = true)
    public DomainResponses.MoodStatsResponse stats(Instant from, Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(7, ChronoUnit.DAYS) : from;
        List<MoodLog> logs = moodLogRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtAsc(currentUser(), start, end);
        if (logs.isEmpty()) {
            return new DomainResponses.MoodStatsResponse(start, end, 0, 0, 0, 0, "stable");
        }
        double avgBefore = logs.stream().mapToInt(MoodLog::getBeforeLevel).average().orElse(0);
        double avgAfter = logs.stream().mapToInt(MoodLog::getAfterLevel).average().orElse(0);
        double improvement = avgBefore == 0 ? 0 : ((avgAfter - avgBefore) / avgBefore) * 100;
        String trend = improvement > 5 ? "improving" : improvement < -5 ? "declining" : "stable";
        return new DomainResponses.MoodStatsResponse(start, end, logs.size(), round(avgBefore), round(avgAfter), round(improvement), trend);
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse delete(UUID id) {
        MoodLog moodLog = moodLogRepository.findByIdAndUser(id, currentUser())
            .orElseThrow(() -> new ResourceNotFoundException("Mood log not found"));
        moodLogRepository.delete(moodLog);
        return new AuthResponses.MessageResponse("OK");
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
