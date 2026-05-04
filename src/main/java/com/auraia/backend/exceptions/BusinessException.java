package com.auraia.backend.exceptions;

public class BusinessException extends RuntimeException {

    private final String messageCode;

    public BusinessException(String messageCode) {
        super(messageCode);
        this.messageCode = messageCode;
    }

    public BusinessException(String messageCode, String detail) {
        super(detail);
        this.messageCode = messageCode;
    }

    public String getMessageCode() {
        return messageCode;
    }
}
