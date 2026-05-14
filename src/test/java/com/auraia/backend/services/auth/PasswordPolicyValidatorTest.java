package com.auraia.backend.services.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auraia.backend.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> validator.validate("StrongPassword123!"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1!", "lowercaseonly123!", "UPPERCASEONLY123!", "NoNumberSymbol!", "NoSymbol1234"})
    void rejectsWeakPasswords(String password) {
        assertThrows(BusinessException.class, () -> validator.validate(password));
    }
}
