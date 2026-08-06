package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/stats")
    public ResponseWrapper<AdminDashboardStatsDto> getDashboardStats() {
        return new ResponseWrapper<>(adminService.getDashboardStats(),"Dashboard stats", HttpStatus.OK.value(), true);
    }

    @GetMapping("/audit-logs")
    public ResponseWrapper<Page<AuditLogDto>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String action) {

        if (action != null && !action.isBlank()) {
            return new ResponseWrapper<>(adminService.getAuditLogsByAction(action, page, size),"Audit logs",HttpStatus.OK.value(), true);
        }
        return new ResponseWrapper<>(adminService.getAllAuditLogs(page, size),"All audit logs",HttpStatus.OK.value(), true);
    }

    @PutMapping("/wallets/freeze")
    public ResponseWrapper<WalletDto> freezeWallet(@RequestParam String phoneNumber) {
        return new ResponseWrapper<>(adminService.freezeWalletByPhoneNumber(phoneNumber),"Wallet freezed",HttpStatus.OK.value(),true);
    }

    @PutMapping("/wallets/unfreeze")
    public ResponseWrapper<WalletDto> unfreezeWallet(@RequestParam String phoneNumber) {
        return new ResponseWrapper<>(adminService.unfreezeWalletByPhoneNumber(phoneNumber),"Wallet Unfreezed",HttpStatus.OK.value(),true);
    }
    @GetMapping("/users")
    public ResponseWrapper<Page<AdminUserResponseDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminUserResponseDto> usersPage = (search != null && !search.isBlank())
                ? adminService.searchUsers(search, page, size)
                : adminService.getAllUsers(page, size);

        return new ResponseWrapper<>(
                usersPage,
                "Users retrieved successfully",
                HttpStatus.OK.value(),
                true
        );
    }
    @GetMapping("/settings/daily-limit")
    public ResponseWrapper<BigDecimal> getGlobalDailyLimit() {
        return new ResponseWrapper<>(
                adminService.getGlobalDailyLimit(),
                "Global daily limit retrieved successfully",
                HttpStatus.OK.value(),
                true
        );
    }

    @PutMapping("/settings/daily-limit")
    public ResponseWrapper<BigDecimal> updateGlobalDailyLimit(@Valid @RequestBody GlobalLimitRequest request) {
        BigDecimal updatedLimit = adminService.updateGlobalDailyLimit(request.getDailyLimit());
        return new ResponseWrapper<>(
                updatedLimit,
                "Global daily limit updated successfully",
                HttpStatus.OK.value(),
                true
        );
    }


}