package com.auraia.backend.services.privacy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ContentCryptoService {

    String encrypt(UUID userId, String scope, String plainText);

    String decrypt(UUID userId, String scope, String cipherText);

    Object encryptJson(UUID userId, String scope, Object value);

    Object decryptJson(UUID userId, String scope, Object value);

    List<String> searchTokens(UUID userId, String... values);

    boolean isEncrypted(String value);

    boolean isEnabled();

    @SuppressWarnings("unchecked")
    default Map<String, Object> encryptJsonMap(UUID userId, String scope, Map<String, Object> value) {
        return (Map<String, Object>) encryptJson(userId, scope, value);
    }

    @SuppressWarnings("unchecked")
    default Map<String, Object> decryptJsonMap(UUID userId, String scope, Map<String, Object> value) {
        return (Map<String, Object>) decryptJson(userId, scope, value);
    }
}
