
package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.enums.WalletStatus;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.AuditLog;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.mapper.TransactionMapper;
import com.project.digitalwallet.mapper.WalletMapper;
import com.project.digitalwallet.repository.AuditLogRepository;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.AdminService;
import com.project.digitalwallet.service.AuditLogService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest httpServletRequest;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${wallet.default-daily-limit}")
    private BigDecimal globalDailyLimit;
    @Override
    public BigDecimal getGlobalDailyLimit() {
        return this.globalDailyLimit;
    }

    @Transactional(readOnly = true)
    @Override
    public AdminDashboardStatsDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalWallets = walletRepository.count();
        long activeWallets = walletRepository.countByStatus(WalletStatus.ACTIVE);
        long frozenWallets = walletRepository.countByStatus(WalletStatus.FROZEN);
        BigDecimal totalBalance = walletRepository.findTotalSystemBalance().orElse(BigDecimal.ZERO);
        long totalTransactions = transactionRepository.count();

        return AdminDashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .totalWallets(totalWallets)
                .activeWallets(activeWallets)
                .frozenWallets(frozenWallets)
                .totalSystemBalance(totalBalance)
                .totalTransactions(totalTransactions)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AuditLogDto> getAllAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findAll(pageable).map(this::mapToAuditLogDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AuditLogDto> getAuditLogsByAction(String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable).map(this::mapToAuditLogDto);
    }

    @Transactional
    @Override
    public WalletDto freezeWalletByPhoneNumber(String phoneNumber) {
        Wallet wallet = walletRepository.findByUserPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for phone number: " + phoneNumber));

        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new IllegalStateException("Wallet is already frozen.");
        }

        wallet.setStatus(WalletStatus.FROZEN);
        Wallet saved = walletRepository.save(wallet);

        auditLogService.logEvent(
                wallet.getUser().getId(),
                "ADMIN_FREEZE_WALLET",
                "Admin froze wallet for phone number: " + phoneNumber,
                httpServletRequest
        );

        return WalletMapper.toWalletDto(saved);
    }

    @Transactional
    @Override
    public WalletDto unfreezeWalletByPhoneNumber(String phoneNumber) {
        Wallet wallet = walletRepository.findByUserPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for phone number: " + phoneNumber));

        if (wallet.getStatus() == WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is already active.");
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        Wallet saved = walletRepository.save(wallet);

        auditLogService.logEvent(
                wallet.getUser().getId(),
                "ADMIN_UNFREEZE_WALLET",
                "Admin unfroze wallet for phone number: " + phoneNumber,
                httpServletRequest
        );

        return WalletMapper.toWalletDto(saved);
    }

    private AuditLogDto mapToAuditLogDto(AuditLog log) {
        InetAddress local = null;
        try {
            // Get local machine IP
            local = InetAddress.getLocalHost();
            System.out.println("Local IP: " + local.getHostAddress());

            // Get IP for a domain name
            InetAddress remote = InetAddress.getByName("://example.com");
            System.out.println("Remote IP: " + remote.getHostAddress());
        } catch (Exception e) {
            System.out.println("Error resolving IP");
        }

        System.out.println(local.getHostAddress());
        return AuditLogDto.builder()

                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userPhoneNumber(log.getUser() != null ? log.getUser().getPhoneNumber() : "N/A")
                .action(log.getAction())
                .description(log.getDescription())
                .ipAddress(local.getHostAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserResponseDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(pageable).map(this::mapToAdminUserResponseDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserResponseDto> searchUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.searchUsers(query, pageable).map(this::mapToAdminUserResponseDto);
    }


    private AdminUserResponseDto mapToAdminUserResponseDto(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElse(null);

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                " " +
                (user.getLastName() != null ? user.getLastName() : "");

        return AdminUserResponseDto.builder()
                .id(user.getId())
                .fullName(fullName.trim())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .walletNumber(wallet != null ? wallet.getWalletNumber() : "N/A")
                .walletStatus(wallet != null ? wallet.getStatus() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
    @Transactional
    @Override
    public void resetUserTransactionPin(AdminResetPinRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("User not found with phone number: " + request.getPhoneNumber()));

        // Encode and set the new PIN
        user.setTransactionPin(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);

        // Audit log
        auditLogService.logEvent(
                user.getId(),
                "ADMIN_PIN_RESET",
                String.format("Transaction PIN force-reset by admin for phone number: %s", request.getPhoneNumber()),
                httpServletRequest
        );
        eventPublisher.publishEvent(new WalletTransactionEvent(
                user.getId(),
                user.getPhoneNumber(),
                NotificationType.SECURITY_ALERT, // or NotificationType.PIN_RESET / NotificationType.SYSTEM_ALERT
                BigDecimal.ZERO,
                "NPR",
                "ADMIN-PIN-" + System.currentTimeMillis(),
                "Your transaction PIN has been reset by an administrator. Please contact support if you did not request this."
        ));
    }
    @Transactional
    @Override
    public void changeUserPassword(AdminChangePasswordRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("User not found with phone number: " + request.getPhoneNumber()));

        // Encode and set new account password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Audit log
        auditLogService.logEvent(
                user.getId(),
                "ADMIN_PASSWORD_CHANGE",
                String.format("Account password force-changed by admin for phone number: %s", request.getPhoneNumber()),
                httpServletRequest
        );
        eventPublisher.publishEvent(new WalletTransactionEvent(
                user.getId(),
                user.getPhoneNumber(),
                NotificationType.SECURITY_ALERT,
                BigDecimal.ZERO,
                "NPR",
                "ADMIN-PWD-" + System.currentTimeMillis(),
                "Your account password has been updated by an administrator. Please contact support immediately if you did not request this."
        ));
    }

}
