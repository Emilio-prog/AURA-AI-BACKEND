package com.auraia.backend.services.diary;

import com.auraia.backend.exceptions.BusinessException;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private static final int MAX_TAGS = 12;
    private static final int MIN_TAG_LENGTH = 2;
    private static final int MAX_TAG_LENGTH = 32;
    private static final Pattern TAG_SEPARATOR = Pattern.compile(",");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;
    private final DiaryEntryMapper diaryEntryMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DomainResponses.DiaryEntryResponse> list(Instant from, Instant to, String query, List<String> tags, Pageable pageable) {
        User user = currentUser();
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now().plusSeconds(315360000) : to;
        String normalizedQuery = blankToNull(query);
        List<String> normalizedTags = normalizeTags(tags);
        if (normalizedQuery == null && normalizedTags.isEmpty()) {
            return PageResponse.from(diaryEntryRepository.findByUserAndCreatedAtBetween(user, start, end, pageable)
                .map(diaryEntryMapper::toResponse));
        }
        return PageResponse.from(searchEntries(user.getId(), start, end, normalizedQuery, normalizedTags, pageable)
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
            .tags(normalizeTags(request.tags()))
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
        entry.setTags(normalizeTags(request.tags()));
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

    @SuppressWarnings("unchecked")
    private Page<DiaryEntry> searchEntries(UUID userId, Instant start, Instant end, String query, List<String> tags, Pageable pageable) {
        StringBuilder where = new StringBuilder("""
            from diary_entries d
            where d.user_id = :userId
              and d.created_at between :start and :end
            """);
        if (query != null) {
            where.append("""
                
                  and to_tsvector('spanish', coalesce(d.title, '') || ' ' || d.content)
                    @@ websearch_to_tsquery('spanish', :query)
                """);
        }
        if (!tags.isEmpty()) {
            where.append("\n  and d.tags @> array[");
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) {
                    where.append(", ");
                }
                where.append(":tag").append(i);
            }
            where.append("]::text[]\n");
        }

        Query dataQuery = entityManager.createNativeQuery("select d.* " + where + orderBy(pageable.getSort()), DiaryEntry.class);
        Query countQuery = entityManager.createNativeQuery("select count(*) " + where);
        bindSearchParameters(dataQuery, userId, start, end, query, tags);
        bindSearchParameters(countQuery, userId, start, end, query, tags);
        dataQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
        dataQuery.setMaxResults(pageable.getPageSize());

        List<DiaryEntry> entries = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new PageImpl<>(entries, pageable, total);
    }

    private void bindSearchParameters(Query nativeQuery, UUID userId, Instant start, Instant end, String query, List<String> tags) {
        nativeQuery.setParameter("userId", userId);
        nativeQuery.setParameter("start", start);
        nativeQuery.setParameter("end", end);
        if (query != null) {
            nativeQuery.setParameter("query", query);
        }
        for (int i = 0; i < tags.size(); i++) {
            nativeQuery.setParameter("tag" + i, tags.get(i));
        }
    }

    private String orderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return " order by d.created_at desc";
        }
        List<String> clauses = new ArrayList<>();
        for (Sort.Order order : sort) {
            String column = sortColumn(order.getProperty());
            if (column != null) {
                clauses.add(column + (order.isAscending() ? " asc" : " desc") + " nulls last");
            }
        }
        return clauses.isEmpty() ? " order by d.created_at desc" : " order by " + String.join(", ", clauses);
    }

    private String sortColumn(String property) {
        return switch (property) {
            case "createdAt" -> "d.created_at";
            case "updatedAt" -> "d.updated_at";
            case "title" -> "lower(d.title)";
            case "moodScore" -> "d.mood_score";
            case "moodLabel" -> "lower(d.mood_label)";
            default -> null;
        };
    }

    private List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String rawTag : rawTags) {
            if (rawTag == null) {
                continue;
            }
            for (String part : TAG_SEPARATOR.split(rawTag)) {
                String tag = normalizeTag(part);
                if (tag.isBlank()) {
                    continue;
                }
                if (tag.length() < MIN_TAG_LENGTH || tag.length() > MAX_TAG_LENGTH) {
                    throw new BusinessException("error.diary_tag_invalid");
                }
                normalized.add(tag);
                if (normalized.size() > MAX_TAGS) {
                    throw new BusinessException("error.diary_tag_invalid");
                }
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeTag(String value) {
        String tag = value.trim();
        while (tag.startsWith("#")) {
            tag = tag.substring(1).trim();
        }
        return WHITESPACE.matcher(tag).replaceAll("-").toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
