package com.auraia.backend.services.privacy;

import com.auraia.backend.config.AppProperties;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AesGcmContentCryptoService implements ContentCryptoService {

    private static final String ENVELOPE_PREFIX = "auraenc:v1:";
    private static final String TOKEN_PREFIX = "auratok:v1:";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String AES_ALGO = "AES";
    private static final String AES_GCM_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9]+");
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();

    private final AppProperties properties;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();
    private byte[] masterKey;

    @PostConstruct
    void initialize() {
        String configuredKey = properties.getContentEncryption().getKey();
        boolean required = properties.getContentEncryption().isRequired()
            || environment.acceptsProfiles(Profiles.of("prod"));
        if (configuredKey == null || configuredKey.isBlank()) {
            if (required) {
                throw new IllegalStateException("CONTENT_ENCRYPTION_KEY is required when content encryption is required");
            }
            return;
        }
        masterKey = decodeKey(configuredKey.trim());
        if (masterKey.length < 32) {
            throw new IllegalStateException("CONTENT_ENCRYPTION_KEY must contain at least 32 bytes of entropy");
        }
    }

    @Override
    public String encrypt(UUID userId, String scope, String plainText) {
        if (plainText == null || isEncrypted(plainText) || !isEnabled()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, keyFor(userId, scope), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(aad(userId, scope));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return ENVELOPE_PREFIX + B64.encodeToString(iv) + ":" + B64.encodeToString(encrypted);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt content", ex);
        }
    }

    @Override
    public String decrypt(UUID userId, String scope, String cipherText) {
        if (cipherText == null || !isEncrypted(cipherText) || !isEnabled()) {
            return cipherText;
        }
        String[] parts = cipherText.substring(ENVELOPE_PREFIX.length()).split(":", 2);
        if (parts.length != 2) {
            throw new IllegalStateException("Invalid encrypted content envelope");
        }
        try {
            byte[] iv = B64_DECODER.decode(parts[0]);
            byte[] encrypted = B64_DECODER.decode(parts[1]);
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, keyFor(userId, scope), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(aad(userId, scope));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to decrypt content", ex);
        }
    }

    @Override
    public Object encryptJson(UUID userId, String scope, Object value) {
        return transformJson(userId, scope, value, true);
    }

    @Override
    public Object decryptJson(UUID userId, String scope, Object value) {
        return transformJson(userId, scope, value, false);
    }

    @Override
    public List<String> searchTokens(UUID userId, String... values) {
        if (!isEnabled() || values == null || values.length == 0) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            for (String part : TOKEN_SPLIT.split(normalizeForSearch(value))) {
                if (part.length() >= 2) {
                    normalized.add(part);
                }
            }
        }
        if (normalized.isEmpty()) {
            return List.of();
        }
        SecretKeySpec key = keyFor(userId, "diary-search");
        return normalized.stream()
            .map(token -> TOKEN_PREFIX + B64.encodeToString(hmac(key.getEncoded(), token.getBytes(StandardCharsets.UTF_8))))
            .toList();
    }

    @Override
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENVELOPE_PREFIX);
    }

    @Override
    public boolean isEnabled() {
        return masterKey != null;
    }

    private Object transformJson(UUID userId, String scope, Object value, boolean encrypt) {
        if (value instanceof String text) {
            return encrypt ? encrypt(userId, scope, text) : decrypt(userId, scope, text);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> transformed = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> transformed.put(String.valueOf(key), transformJson(userId, scope, nestedValue, encrypt)));
            return transformed;
        }
        if (value instanceof List<?> list) {
            List<Object> transformed = new ArrayList<>(list.size());
            for (Object item : list) {
                transformed.add(transformJson(userId, scope, item, encrypt));
            }
            return transformed;
        }
        return value;
    }

    private SecretKeySpec keyFor(UUID userId, String scope) {
        byte[] extracted = hmac("aura-content-encryption:v1".getBytes(StandardCharsets.UTF_8), masterKey);
        byte[] expanded = hmac(extracted, ("field:" + userId + ":" + scope).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(expanded, AES_ALGO);
    }

    private byte[] aad(UUID userId, String scope) {
        return ("auraenc:v1:" + userId + ":" + scope).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] hmac(byte[] key, byte[] value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            return mac.doFinal(value);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to calculate HMAC", ex);
        }
    }

    private byte[] decodeKey(String configuredKey) {
        if (configuredKey.startsWith("base64:")) {
            return Base64.getDecoder().decode(configuredKey.substring("base64:".length()));
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Treat non-base64 values as raw secret material.
        }
        return configuredKey.getBytes(StandardCharsets.UTF_8);
    }

    private String normalizeForSearch(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(java.util.Locale.ROOT);
    }
}
