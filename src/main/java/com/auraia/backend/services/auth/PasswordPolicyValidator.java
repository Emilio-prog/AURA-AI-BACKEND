package com.auraia.backend.services.auth;

import com.auraia.backend.exceptions.BusinessException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    public void validate(String password) {
        if (password == null
            || password.length() < 12
            || !UPPER.matcher(password).find()
            || !LOWER.matcher(password).find()
            || !DIGIT.matcher(password).find()
            || !SYMBOL.matcher(password).find()) {
            throw new BusinessException("error.password_policy");
        }
    }
}
