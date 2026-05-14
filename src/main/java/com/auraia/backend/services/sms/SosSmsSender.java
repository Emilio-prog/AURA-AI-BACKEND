package com.auraia.backend.services.sms;

public interface SosSmsSender {

    SosSmsResult send(String toPhoneNumber, String body);
}
