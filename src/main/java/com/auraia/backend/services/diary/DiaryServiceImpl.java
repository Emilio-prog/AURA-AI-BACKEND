package com.auraia.backend.services.diary;

import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.entities.DiaryEntry;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
import com.auraia.backend.services.privacy.ContentCryptoService;
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
    private static final String SCOPE_TITLE = "diary.title";
    private static final String SCOPE_CONTENT = "diary.content";
    private static final String SCOPE_MOOD_LABEL = "diary.mood-label";

    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final ContentCryptoService contentCryptoService;

    @Override
    @Transactional
    public PageResponse<DomainResponses.DiaryEntryResponse> list(Instant from, Instant to, String query, List<String> tags, Pageable pageable) {
        User user = currentUser();
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now().plusSeconds(315360000) : to;
        String normalizedQuery = blankToNull(query);
        List<String> normalizedTags = normalizeTags(tags);
        List<String> queryTokens = normalizedQuery == null ? List.of() : contentCryptoService.searchTokens(user.getId(), normalizedQuery);
        if (normalizedQuery == null && normalizedTags.isEmpty()) {
            return PageResponse.from(diaryEntryRepository.findByUserAndCreatedAtBetween(user, start, end, pageable)
                .map(entry -> decryptAndProtect(entry, user)));
        }
        return PageResponse.from(searchEntries(user.getId(), start, end, normalizedQuery, queryTokens, normalizedTags, pageable)
            .map(entry -> decryptAndProtect(entry, user)));
    }

    @Override
    @Transactional
    public DomainResponses.DiaryEntryResponse get(UUID id) {
        User user = currentUser();
        return decryptAndProtect(findOwned(id, user), user);
    }

    @Override
    @Transactional
    public DomainResponses.DiaryEntryResponse create(DomainRequests.DiaryEntryRequest request) {
        User user = currentUser();
        String title = blankToNull(request.title());
        String content = request.content().trim();
        String moodLabel = blankToNull(request.moodLabel());
        DiaryEntry entry = DiaryEntry.builder()
            .user(user)
            .title(contentCryptoService.encrypt(user.getId(), SCOPE_TITLE, title))
            .content(contentCryptoService.encrypt(user.getId(), SCOPE_CONTENT, content))
            .moodScore(request.moodScore())
            .moodLabel(contentCryptoService.encrypt(user.getId(), SCOPE_MOOD_LABEL, moodLabel))
            .tags(normalizeTags(request.tags()))
            .searchTokens(contentCryptoService.searchTokens(user.getId(), title, content))
            .build();
        DiaryEntry saved = diaryEntryRepository.save(entry);
        return response(saved, title, content, moodLabel);
    }

    @Override
    @Transactional
    public DomainResponses.DiaryEntryResponse update(UUID id, DomainRequests.DiaryEntryRequest request) {
        User user = currentUser();
        String title = blankToNull(request.title());
        String content = request.content().trim();
        String moodLabel = blankToNull(request.moodLabel());
        DiaryEntry entry = findOwned(id, user);
        entry.setTitle(contentCryptoService.encrypt(user.getId(), SCOPE_TITLE, title));
        entry.setContent(contentCryptoService.encrypt(user.getId(), SCOPE_CONTENT, content));
        entry.setMoodScore(request.moodScore());
        entry.setMoodLabel(contentCryptoService.encrypt(user.getId(), SCOPE_MOOD_LABEL, moodLabel));
        entry.setTags(normalizeTags(request.tags()));
        entry.setSearchTokens(contentCryptoService.searchTokens(user.getId(), title, content));
        DiaryEntry saved = diaryEntryRepository.save(entry);
        return response(saved, title, content, moodLabel);
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse delete(UUID id) {
        diaryEntryRepository.delete(findOwned(id, currentUser()));
        return new AuthResponses.MessageResponse("OK");
    }

    private DiaryEntry findOwned(UUID id) {
        return findOwned(id, currentUser());
    }

    private DiaryEntry findOwned(UUID id, User user) {
        return diaryEntryRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Diary entry not found"));
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @SuppressWarnings("unchecked")
    private Page<DiaryEntry> searchEntries(UUID userId, Instant start, Instant end, String query, List<String> queryTokens, List<String> tags, Pageable pageable) {
        StringBuilder where = new StringBuilder("""
            from diary_entries d
            where d.user_id = :userId
              and d.created_at between :start and :end
            """);
        if (query != null) {
            where.append("\n  and (");
            if (!queryTokens.isEmpty()) {
                where.append("d.search_tokens @> array[");
                for (int i = 0; i < queryTokens.size(); i++) {
                    if (i > 0) {
                        where.append(", ");
                    }
                    where.append(":queryToken").append(i);
                }
                where.append("]::text[] or ");
            }
            where.append("to_tsvector('spanish', coalesce(d.title, '') || ' ' || d.content) @@ websearch_to_tsquery('spanish', :query))\n");
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
        bindSearchParameters(dataQuery, userId, start, end, query, queryTokens, tags);
        bindSearchParameters(countQuery, userId, start, end, query, queryTokens, tags);
        dataQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
        dataQuery.setMaxResults(pageable.getPageSize());

        List<DiaryEntry> entries = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new PageImpl<>(entries, pageable, total);
    }

    private void bindSearchParameters(Query nativeQuery, UUID userId, Instant start, Instant end, String query, List<String> queryTokens, List<String> tags) {
        nativeQuery.setParameter("userId", userId);
        nativeQuery.setParameter("start", start);
        nativeQuery.setParameter("end", end);
        if (query != null) {
            nativeQuery.setParameter("query", query);
            for (int i = 0; i < queryTokens.size(); i++) {
                nativeQuery.setParameter("queryToken" + i, queryTokens.get(i));
            }
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

    private DomainResponses.DiaryEntryResponse decryptAndProtect(DiaryEntry entry, User user) {
        String title = contentCryptoService.decrypt(user.getId(), SCOPE_TITLE, entry.getTitle());
        String content = contentCryptoService.decrypt(user.getId(), SCOPE_CONTENT, entry.getContent());
        String moodLabel = contentCryptoService.decrypt(user.getId(), SCOPE_MOOD_LABEL, entry.getMoodLabel());
        boolean changed = false;
        if (contentCryptoService.isEnabled()) {
            String encryptedTitle = encryptLegacyValue(user.getId(), SCOPE_TITLE, entry.getTitle(), title);
            String encryptedContent = encryptLegacyValue(user.getId(), SCOPE_CONTENT, entry.getContent(), content);
            String encryptedMoodLabel = encryptLegacyValue(user.getId(), SCOPE_MOOD_LABEL, entry.getMoodLabel(), moodLabel);
            List<String> searchTokens = contentCryptoService.searchTokens(user.getId(), title, content);
            if (!same(entry.getTitle(), encryptedTitle)) {
                entry.setTitle(encryptedTitle);
                changed = true;
            }
            if (!same(entry.getContent(), encryptedContent)) {
                entry.setContent(encryptedContent);
                changed = true;
            }
            if (!same(entry.getMoodLabel(), encryptedMoodLabel)) {
                entry.setMoodLabel(encryptedMoodLabel);
                changed = true;
            }
            if (!sameList(entry.getSearchTokens(), searchTokens)) {
                entry.setSearchTokens(searchTokens);
                changed = true;
            }
            if (changed) {
                diaryEntryRepository.save(entry);
            }
        }
        return response(entry, title, content, moodLabel);
    }

    private DomainResponses.DiaryEntryResponse response(DiaryEntry entry, String title, String content, String moodLabel) {
        return new DomainResponses.DiaryEntryResponse(
            entry.getId(),
            title,
            content,
            entry.getMoodScore(),
            moodLabel,
            entry.getTags() == null ? List.of() : List.copyOf(entry.getTags()),
            entry.getCreatedAt(),
            entry.getUpdatedAt()
        );
    }

    private boolean same(String left, String right) {
        return java.util.Objects.equals(left, right);
    }

    private boolean sameList(List<String> left, List<String> right) {
        return java.util.Objects.equals(left == null ? List.of() : left, right == null ? List.of() : right);
    }

    private String encryptLegacyValue(UUID userId, String scope, String storedValue, String plainText) {
        return storedValue != null && !contentCryptoService.isEncrypted(storedValue)
            ? contentCryptoService.encrypt(userId, scope, plainText)
            : storedValue;
    }
}
