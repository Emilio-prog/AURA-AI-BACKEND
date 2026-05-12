package com.auraia.backend.services.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.auraia.backend.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class AesGcmContentCryptoServiceTest {

    @Test
    void encryptsAndDecryptsFieldContent() {
        AesGcmContentCryptoService service = serviceWithKey(false);
        UUID userId = UUID.randomUUID();

        String encrypted = service.encrypt(userId, "diary.content", "Contenido privado");

        assertThat(encrypted).startsWith("auraenc:v1:");
        assertThat(encrypted).isNotEqualTo("Contenido privado");
        assertThat(service.decrypt(userId, "diary.content", encrypted)).isEqualTo("Contenido privado");
    }

    @Test
    void doesNotEncryptEnvelopeTwice() {
        AesGcmContentCryptoService service = serviceWithKey(false);
        UUID userId = UUID.randomUUID();
        String encrypted = service.encrypt(userId, "chat.title", "Titulo");

        assertThat(service.encrypt(userId, "chat.title", encrypted)).isSameAs(encrypted);
    }

    @Test
    void createsDeterministicSearchTokensPerUser() {
        AesGcmContentCryptoService service = serviceWithKey(false);
        UUID userId = UUID.randomUUID();

        List<String> first = service.searchTokens(userId, "Sueño calma", "calma");
        List<String> second = service.searchTokens(userId, "sueno CALMA");

        assertThat(first).isEqualTo(second);
        assertThat(service.searchTokens(UUID.randomUUID(), "sueno calma")).isNotEqualTo(first);
    }

    @Test
    void missingKeyFailsWhenRequired() {
        AppProperties properties = new AppProperties();
        properties.getContentEncryption().setRequired(true);
        Environment environment = org.mockito.Mockito.mock(Environment.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        AesGcmContentCryptoService service = new AesGcmContentCryptoService(properties, environment);

        assertThatThrownBy(service::initialize)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONTENT_ENCRYPTION_KEY");
    }

    private AesGcmContentCryptoService serviceWithKey(boolean required) {
        AppProperties properties = new AppProperties();
        properties.getContentEncryption().setRequired(required);
        properties.getContentEncryption().setKey("base64:" + Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
        ));
        Environment environment = org.mockito.Mockito.mock(Environment.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        AesGcmContentCryptoService service = new AesGcmContentCryptoService(properties, environment);
        service.initialize();
        return service;
    }
}
