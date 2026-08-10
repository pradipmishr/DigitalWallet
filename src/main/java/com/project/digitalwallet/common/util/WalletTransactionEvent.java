package com.project.digitalwallet.common.util;


import com.project.digitalwallet.common.enums.NotificationType;

import java.math.BigDecimal;

public record WalletTransactionEvent(
        Long userId,
        String userPhoneNumber,
        NotificationType type,
        BigDecimal amount,
        String currency,
        String referenceId,
        String description
) {}