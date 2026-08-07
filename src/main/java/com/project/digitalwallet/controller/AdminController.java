package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.AdminService;
import com.project.digitalwallet.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final TransactionService transactionService;

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
    @PutMapping("/users/reset-pin")
    public ResponseWrapper<String> resetUserPin(@Valid @RequestBody AdminResetPinRequest request) {
        adminService.resetUserTransactionPin(request);
        return new ResponseWrapper<>(
                "Transaction PIN reset successfully for phone number: " + request.getPhoneNumber(),
                "SUCCESS",
                HttpStatus.OK.value(),
                true
        );
    }
    @PutMapping("/users/reset-password")
    public ResponseWrapper<String> changeUserPassword(@Valid @RequestBody AdminChangePasswordRequest request) {
        adminService.changeUserPassword(request);
        return new ResponseWrapper<>(
                "Password changed successfully for user with phone number: " + request.getPhoneNumber(),
                "SUCCESS",
                HttpStatus.OK.value(),
                true
        );
    }

    @PostMapping("/transaction/search")
    public ResponseWrapper<Page<TransactionDto>> searchTransactions(
            @RequestBody AdminTransactionSearchRequest request) {

        Page<TransactionDto> results = transactionService.searchTransactionsForAdmin(request);
        return new ResponseWrapper<>(
                results,
                "Transactions fetched successfully",
                HttpStatus.OK.value(),
                true
        );
    }

    @PostMapping("/transaction/reverse")
    public ResponseWrapper<TransactionDto> reverseTransaction(
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            @Valid @RequestBody AdminReverseTransactionRequest request) {

        UserDto adminUserDto = UserMapper.toUserDto(adminPrincipal.getUser());
        TransactionDto result = transactionService.reverseTransaction(adminUserDto, request);

        return new ResponseWrapper<>(
                result,
                "Transaction reversed successfully.",
                HttpStatus.OK.value(),
                true
        );
    }
}