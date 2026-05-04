package com.auraia.backend.services.diary;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.mappers.DiaryEntryMapper;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.entities.DiaryEntry;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;
    private final DiaryEntryMapper diaryEntryMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DomainResponses.DiaryEntryResponse> list(Instant from, Instant to, Pageable pageable) {
        User user = currentUser();
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now().plusSeconds(315360000) : to;
        return PageResponse.from(diaryEntryRepository.findByUserAndCreatedAtBetween(user, start, end, pageable)
            .map(diaryEntryMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public DomainResponses.DiaryEntryResponse get(UUID id) {
        return diaryEntryMapper.toResponse(findOwned(id));
    }

    @Override
    @Transactional
    public DomainResponses.DiaryEntryResponse create(DomainRequests.DiaryEntryRequest request) {
        DiaryEntry entry = DiaryEntry.builder()
            .user(currentUser())
            .title(blankToNull(request.title()))
            .content(request.content().trim())
            .moodScore(request.moodScore())
            .moodLabel(blankToNull(request.moodLabel()))
            .build();
        return diaryEntryMapper.toResponse(diaryEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public DomainResponses.DiaryEntryResponse update(UUID id, DomainRequests.DiaryEntryRequest request) {
        DiaryEntry entry = findOwned(id);
        entry.setTitle(blankToNull(request.title()));
        entry.setContent(request.content().trim());
        entry.setMoodScore(request.moodScore());
        entry.setMoodLabel(blankToNull(request.moodLabel()));
        return diaryEntryMapper.toResponse(diaryEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse delete(UUID id) {
        diaryEntryRepository.delete(findOwned(id));
        return new AuthResponses.MessageResponse("OK");
    }

    private DiaryEntry findOwned(UUID id) {
        return diaryEntryRepository.findByIdAndUser(id, currentUser())
            .orElseThrow(() -> new ResourceNotFoundException("Diary entry not found"));
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
