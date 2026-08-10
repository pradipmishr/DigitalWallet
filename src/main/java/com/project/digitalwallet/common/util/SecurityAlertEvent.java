package com.project.digitalwallet.common.util;


public record SecurityAlertEvent(
        Long userId,
        String alertTitle,
        String message,
        String deviceOrIpDetails
) {}