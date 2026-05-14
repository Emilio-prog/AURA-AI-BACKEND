package com.auraia.backend.services.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.models.enums.NotificationStatus;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.models.enums.Theme;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserExportPdfServiceTest {

    static {
        try {
            Path cacheDir = Path.of("target", "pdfbox-cache");
            Files.createDirectories(cacheDir);
            System.setProperty("pdfbox.fontcache", cacheDir.toAbsolutePath().toString());
        } catch (Exception ignored) {
            // Cache setup is best-effort for quieter local tests.
        }
    }

    private final UserExportPdfService service = new UserExportPdfService();

    @Test
    void renderCreatesPdfWithExportedContent() {
        UUID userId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-11T10:00:00Z");
        UserResponses.ExportDataResponse export = new UserResponses.ExportDataResponse(
            now,
            new UserResponses.UserResponse(userId, "Emilio", "emilio@example.com", Role.USER, Plan.FREE, true, now, now),
            new DomainResponses.UserSettingsResponse(UUID.randomUUID(), Theme.SYSTEM, "es", "Europe/Madrid", Map.of("enabled", true), now, null),
            List.of(new DomainResponses.DiaryEntryResponse(UUID.randomUUID(), "Dia dificil", "Respire y escribi.", 6, "calma", List.of("calma"), now, null)),
            List.of(new DomainResponses.MoodLogResponse(UUID.randomUUID(), 4, 7, "Mejor despues", now, now, null)),
            List.of(new DomainResponses.ChatSessionResponse(UUID.randomUUID(), "Aura", List.of(Map.of("role", "user", "content", "hola")), now, null)),
            List.of(new DomainResponses.ContactResponse(contactId, "Ana", "+34 600 000 000", "Hermana", 1, true, true, now, null)),
            List.of(new DomainResponses.PanicAlertResponse(
                UUID.randomUUID(),
                now,
                null,
                "Necesito ayuda",
                Map.of(),
                List.of(new DomainResponses.PanicNotificationResponse(UUID.randomUUID(), contactId, "Ana", "SMS", NotificationStatus.SENT, "SMS sent", now)),
                now,
                null
            )),
            Map.of("format", "aura-export-v1")
        );

        byte[] pdf = service.render(export);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
