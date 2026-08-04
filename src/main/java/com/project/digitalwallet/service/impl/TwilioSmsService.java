package com.project.digitalwallet.service.impl;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioSmsService {

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    public void sendSms(String toPhoneNumber, String messageBody) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),  // To
                    new PhoneNumber(fromPhoneNumber), // From (Your Twilio Number)
                    messageBody                      // Body
            ).create();

            log.info("SMS sent successfully to {}. Message SID: {}", toPhoneNumber, message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to {}: {}", toPhoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS code. Please try again later.", e);
        }
    }
}