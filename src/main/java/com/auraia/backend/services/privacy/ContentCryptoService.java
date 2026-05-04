package com.auraia.backend.services.privacy;

public interface ContentCryptoService {

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
