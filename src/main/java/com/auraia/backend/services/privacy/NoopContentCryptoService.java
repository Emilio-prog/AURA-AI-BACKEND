package com.auraia.backend.services.privacy;

import org.springframework.stereotype.Service;

@Service
public class NoopContentCryptoService implements ContentCryptoService {

    @Override
    public String encrypt(String plainText) {
        return plainText;
    }

    @Override
    public String decrypt(String cipherText) {
        return cipherText;
    }
}
