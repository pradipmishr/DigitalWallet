package com.project.digitalwallet.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminDashboardStatsDto {
    private long totalUsers;
    private long totalWallets;
    private long activeWallets;
    private long frozenWallets;
    private BigDecimal totalSystemBalance;
    private long totalTransactions;
}