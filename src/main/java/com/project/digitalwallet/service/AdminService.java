package com.project.digitalwallet.service;


import com.project.digitalwallet.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface AdminService {
    AdminDashboardStatsDto getDashboardStats();
    Page<AuditLogDto> getAllAuditLogs(int page, int size);
    Page<AuditLogDto> getAuditLogsByAction(String action, int page, int size);
    WalletDto freezeWalletByPhoneNumber(String phoneNumber);
    WalletDto unfreezeWalletByPhoneNumber(String phoneNumber);
    Page<AdminUserResponseDto> getAllUsers(int page, int size);
    Page<AdminUserResponseDto> searchUsers(String query, int page, int size);
    BigDecimal getGlobalDailyLimit();
    void resetUserTransactionPin(AdminResetPinRequest request);
    void changeUserPassword(AdminChangePasswordRequest request);

}