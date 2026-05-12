package com.auraia.backend.services.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.mappers.DiaryEntryMapper;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.entities.DiaryEntry;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DiaryServiceImplTest {

    @Mock
    DiaryEntryRepository diaryEntryRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    DiaryEntryMapper diaryEntryMapper;
    @Mock
    EntityManager entityManager;
    @Mock
    Query dataQuery;
    @Mock
    Query countQuery;

    DiaryServiceImpl service;
    UUID userId;
    User user;

    @BeforeEach
    void setUp() {
        service = new DiaryServiceImpl(diaryEntryRepository, userRepository, diaryEntryMapper, entityManager);
        userId = UUID.randomUUID();
        user = User.builder()
            .email("emilio@example.com")
            .passwordHash("hash")
            .name("Emilio")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .build();
        user.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userId, user.getEmail(), user.getPasswordHash(), true, List.of()),
            null,
            List.of()
        ));
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createNormalizesTagsAndReturnsEmptyWhenMissing() {
        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(diaryEntryMapper.toResponse(any(DiaryEntry.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

        DomainResponses.DiaryEntryResponse response = service.create(new DomainRequests.DiaryEntryRequest(
            "Titulo",
            "Contenido",
            7,
            "CALMA",
            List.of(" Ansiedad ", "#Sueño ligero", "ansiedad", "", "gratitud")
        ));

        assertThat(response.tags()).containsExactly("ansiedad", "sueño-ligero", "gratitud");
        ArgumentCaptor<DiaryEntry> captor = ArgumentCaptor.forClass(DiaryEntry.class);
        verify(diaryEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getTags()).containsExactly("ansiedad", "sueño-ligero", "gratitud");
    }

    @Test
    void rejectsInvalidTagLengthAndTooManyTags() {
        assertThatThrownBy(() -> service.create(new DomainRequests.DiaryEntryRequest(
            null,
            "Contenido",
            null,
            null,
            List.of("x")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessage("error.diary_tag_invalid");

        assertThatThrownBy(() -> service.create(new DomainRequests.DiaryEntryRequest(
            null,
            "Contenido",
            null,
            null,
            List.of("uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce", "trece")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessage("error.diary_tag_invalid");
    }

    @Test
    void listWithQueryAndTagsUsesFullTextAndTagFilter() {
        DiaryEntry entry = DiaryEntry.builder()
            .user(user)
            .title("Respirar")
            .content("Necesito respirar")
            .tags(List.of("ansiedad", "sueño"))
            .build();
        entry.setId(UUID.randomUUID());
        entry.setCreatedAt(Instant.parse("2026-05-12T09:00:00Z"));

        when(entityManager.createNativeQuery(anyString(), eq(DiaryEntry.class))).thenReturn(dataQuery);
        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(20)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of(entry));
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(diaryEntryMapper.toResponse(any(DiaryEntry.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

        DomainResponses.DiaryEntryResponse response = service.list(
            null,
            null,
            " respirar ",
            List.of("Ansiedad", "Sueño"),
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).content().getFirst();

        assertThat(response.tags()).containsExactly("ansiedad", "sueño");
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sqlCaptor.capture(), eq(DiaryEntry.class));
        assertThat(sqlCaptor.getValue())
            .contains("websearch_to_tsquery('spanish', :query)")
            .contains("d.tags @> array[:tag0, :tag1]::text[]")
            .contains("order by d.created_at desc");
        verify(dataQuery).setParameter("query", "respirar");
        verify(dataQuery).setParameter("tag0", "ansiedad");
        verify(dataQuery).setParameter("tag1", "sueño");
    }

    private DomainResponses.DiaryEntryResponse response(DiaryEntry entry) {
        return new DomainResponses.DiaryEntryResponse(
            entry.getId(),
            entry.getTitle(),
            entry.getContent(),
            entry.getMoodScore(),
            entry.getMoodLabel(),
            entry.getTags() == null ? List.of() : List.copyOf(entry.getTags()),
            entry.getCreatedAt(),
            entry.getUpdatedAt()
        );
    }
}
