package com.auraia.backend.services.privacy;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TestContentCryptoService implements ContentCryptoService {

    private static final String PREFIX = "auraenc:v1:test:";

    private final boolean enabled;

    public TestContentCryptoService() {
        this(false);
    }

    public TestContentCryptoService(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String encrypt(UUID userId, String scope, String plainText) {
        if (!enabled || plainText == null || isEncrypted(plainText)) {
            return plainText;
        }
        return PREFIX + plainText;
    }

    @Override
    public String decrypt(UUID userId, String scope, String cipherText) {
        if (!enabled || !isEncrypted(cipherText)) {
            return cipherText;
        }
        return cipherText.substring(PREFIX.length());
    }

    @Override
    public Object encryptJson(UUID userId, String scope, Object value) {
        return transform(userId, scope, value, true);
    }

    @Override
    public Object decryptJson(UUID userId, String scope, Object value) {
        return transform(userId, scope, value, false);
    }

    @Override
    public List<String> searchTokens(UUID userId, String... values) {
        if (!enabled || values == null) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT);
            for (String token : normalized.split("[^a-z0-9]+")) {
                if (token.length() >= 2) {
                    tokens.add("token:" + token);
                }
            }
        }
        return List.copyOf(tokens);
    }

    @Override
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private Object transform(UUID userId, String scope, Object value, boolean encrypt) {
        if (value instanceof String text) {
            return encrypt ? encrypt(userId, scope, text) : decrypt(userId, scope, text);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> transformed = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> transformed.put(String.valueOf(key), transform(userId, scope, nestedValue, encrypt)));
            return transformed;
        }
        if (value instanceof List<?> list) {
            List<Object> transformed = new ArrayList<>(list.size());
            for (Object item : list) {
                transformed.add(transform(userId, scope, item, encrypt));
            }
            return transformed;
        }
        return value;
    }
}
