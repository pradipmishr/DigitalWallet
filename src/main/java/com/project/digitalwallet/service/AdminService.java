package com.project.digitalwallet.service;


import com.project.digitalwallet.dto.AdminDashboardStatsDto;
import com.project.digitalwallet.dto.AuditLogDto;
import com.project.digitalwallet.dto.WalletDto;
import org.springframework.data.domain.Page;

public interface AdminService {
    AdminDashboardStatsDto getDashboardStats();
    Page<AuditLogDto> getAllAuditLogs(int page, int size);
    Page<AuditLogDto> getAuditLogsByAction(String action, int page, int size);
    WalletDto freezeWalletByPhoneNumber(String phoneNumber);
    WalletDto unfreezeWalletByPhoneNumber(String phoneNumber);
}
