package com.project.digitalwallet.common.util;

public interface SmsSender {
    void sendSms(String toPhoneNumber, String message);
}
