package com.project.digitalwallet.common.util;

import com.project.digitalwallet.common.enums.KycStatus;
import com.project.digitalwallet.entity.KycDetails;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.repository.KycDetailsRepository;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransactionLimitValidator {

    private final KycDetailsRepository kycDetailsRepository;
    private final TransactionRepository transactionRepository;
    private final AdminService adminService;

    public void validateTieredLimits(User senderUser, Wallet senderWallet, BigDecimal transferAmount) {
        // 1. Fetch user's KYC status
        Optional<KycDetails> kycOptional = kycDetailsRepository.findByUserId(senderUser.getId());
        KycStatus kycStatus = kycOptional.map(KycDetails::getStatus).orElse(KycStatus.UNVERIFIED);

        boolean isKycVerified = (kycStatus == KycStatus.VERIFIED);

        // 2. Define Limits based on Tier
        BigDecimal dailyLimit = isKycVerified ? adminService.getGlobalDailyLimit() : new BigDecimal("1000.00");
        BigDecimal monthlyLimit = isKycVerified ? null : new BigDecimal("10000.00");

        // 3. Calculate Daily Transferred Sum
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        BigDecimal totalToday = transactionRepository.findTotalTransferredToday(
                senderWallet.getId(),
                startOfDay,
                endOfDay
        );

        BigDecimal projectedDailyTotal = totalToday.add(transferAmount);

        if (projectedDailyTotal.compareTo(dailyLimit) > 0) {
            BigDecimal remainingDailyLimit = dailyLimit.subtract(totalToday);
            if (remainingDailyLimit.compareTo(BigDecimal.ZERO) < 0) {
                remainingDailyLimit = BigDecimal.ZERO;
            }

            if (!isKycVerified) {
                throw new IllegalStateException(String.format(
                        "Unverified accounts are restricted to NPR %.2f daily. Total transferred today: NPR %.2f. Remaining daily limit: NPR %.2f. Please complete KYC verification to unlock higher limits.",
                        dailyLimit, totalToday, remainingDailyLimit
                ));
            } else {
                throw new IllegalStateException(String.format(
                        "Transfer exceeds your daily limit of NPR %.2f. Total transferred today: NPR %.2f. Remaining daily limit: NPR %.2f.",
                        dailyLimit, totalToday, remainingDailyLimit
                ));
            }
        }

        // 4. Validate Monthly Limit for Unverified Users
        if (!isKycVerified) {
            YearMonth currentMonth = YearMonth.now();
            LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

            BigDecimal totalThisMonth = transactionRepository.findTotalTransferredThisMonth(
                    senderWallet.getId(),
                    startOfMonth,
                    endOfMonth
            );

            BigDecimal projectedMonthlyTotal = totalThisMonth.add(transferAmount);

            if (projectedMonthlyTotal.compareTo(monthlyLimit) > 0) {
                BigDecimal remainingMonthlyLimit = monthlyLimit.subtract(totalThisMonth);
                if (remainingMonthlyLimit.compareTo(BigDecimal.ZERO) < 0) {
                    remainingMonthlyLimit = BigDecimal.ZERO;
                }

                throw new IllegalStateException(String.format(
                        "Unverified accounts are restricted to NPR %.2f monthly. Total transferred this month: NPR %.2f. Remaining monthly limit: NPR %.2f. Complete KYC verification for unlimited monthly transfers.",
                        monthlyLimit, totalThisMonth, remainingMonthlyLimit
                ));
            }
        }
    }
}