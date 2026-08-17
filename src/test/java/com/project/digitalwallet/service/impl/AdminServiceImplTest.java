package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.WalletStatus;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.AuditLog;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.repository.AuditLogRepository;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AdminServiceImpl adminService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;


    @Test
    void getDashboardStats_shouldReturnCorrectDashboardStats() {

        // Arrange
        when(userRepository.count()).thenReturn(100L);

        when(walletRepository.count()).thenReturn(80L);

        when(walletRepository.countByStatus(WalletStatus.ACTIVE))
                .thenReturn(70L);

        when(walletRepository.countByStatus(WalletStatus.FROZEN))
                .thenReturn(10L);

        when(walletRepository.findTotalSystemBalance())
                .thenReturn(Optional.of(new BigDecimal("50000.00")));

        when(transactionRepository.count())
                .thenReturn(250L);

        // Act
        AdminDashboardStatsDto result =
                adminService.getDashboardStats();

        // Assert
        assertEquals(100L, result.getTotalUsers());
        assertEquals(80L, result.getTotalWallets());
        assertEquals(70L, result.getActiveWallets());
        assertEquals(10L, result.getFrozenWallets());
        assertEquals(
                new BigDecimal("50000.00"),
                result.getTotalSystemBalance()
        );
        assertEquals(250L, result.getTotalTransactions());
    }
    @Test
    void getAllAuditLogs_shouldUseCorrectPaginationAndSorting() {

        // Arrange
        int page = 0;
        int size = 10;

        AuditLog auditLog = new AuditLog();

        Page<AuditLog> auditLogPage =
                new PageImpl<>(
                        List.of(auditLog),
                        PageRequest.of(page, size),
                        1
                );

        when(auditLogRepository.findAll(any(Pageable.class)))
                .thenReturn(auditLogPage);

        // Act
        Page<AuditLogDto> result =
                adminService.getAllAuditLogs(page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        // Capture the Pageable sent to repository
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(auditLogRepository)
                .findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());

        assertEquals(
                Sort.Direction.DESC,
                pageable.getSort().getOrderFor("createdAt").getDirection()
        );
    }
    @Test
    void getAuditLogsByAction_shouldReturnAuditLogs() {

        // Arrange
        String action = "LOGIN";
        int page = 0;
        int size = 10;

        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAction("LOGIN");
        auditLog.setDescription("User logged in");

        Page<AuditLog> auditLogPage =
                new PageImpl<>(
                        List.of(auditLog),
                        PageRequest.of(page, size),
                        1
                );

        when(auditLogRepository.findByActionOrderByCreatedAtDesc(
                eq(action),
                any(Pageable.class)
        )).thenReturn(auditLogPage);

        // Act
        Page<AuditLogDto> result =
                adminService.getAuditLogsByAction(action, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        verify(auditLogRepository)
                .findByActionOrderByCreatedAtDesc(
                        eq(action),
                        any(Pageable.class)
                );
    }
    @Test
    void getAuditLogsByAction_shouldReturnEmptyPage_whenNoLogsFound() {

        // Arrange
        String action = "LOGIN";

        Page<AuditLog> emptyPage =
                new PageImpl<>(List.of());

        when(auditLogRepository.findByActionOrderByCreatedAtDesc(
                eq(action),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        // Act
        Page<AuditLogDto> result =
                adminService.getAuditLogsByAction(action, 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }
    @Test
    void freezeWalletByPhoneNumber_shouldFreezeWallet() {

        // Arrange
        String phoneNumber = "+9779800000000";

        User user = new User();
        user.setId(1L);

        Wallet wallet = new Wallet();
        wallet.setId(10L);
        wallet.setUser(user);
        wallet.setStatus(WalletStatus.ACTIVE);

        when(walletRepository.findByUserPhoneNumber(phoneNumber))
                .thenReturn(Optional.of(wallet));

        when(walletRepository.save(wallet))
                .thenReturn(wallet);

        // Act
        WalletDto result =
                adminService.freezeWalletByPhoneNumber(phoneNumber);

        // Assert
        assertEquals(WalletStatus.FROZEN, wallet.getStatus());

        verify(walletRepository).save(wallet);

        verify(auditLogService).logEvent(
                eq(1L),
                eq("ADMIN_FREEZE_WALLET"),
                eq("Admin froze wallet for phone number: " + phoneNumber),
                eq(httpServletRequest)
        );
    }
    @Test
    void freezeWalletByPhoneNumber_shouldThrowException_whenWalletNotFound() {

        // Arrange
        String phoneNumber = "9800000000";

        when(walletRepository.findByUserPhoneNumber(phoneNumber))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> adminService.freezeWalletByPhoneNumber(phoneNumber)
                );

        assertEquals(
                "Wallet not found for phone number: " + phoneNumber,
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }
    @Test
    void freezeWalletByPhoneNumber_shouldThrowException_whenWalletAlreadyFrozen() {

        // Arrange
        String phoneNumber = "+9779800000000";

        Wallet wallet = new Wallet();
        wallet.setStatus(WalletStatus.FROZEN);

        when(walletRepository.findByUserPhoneNumber(phoneNumber))
                .thenReturn(Optional.of(wallet));

        // Act & Assert
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> adminService.freezeWalletByPhoneNumber(phoneNumber)
                );

        assertEquals(
                "Wallet is already frozen.",
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }
    @Test
    void unfreezeWalletByPhoneNumber_shouldUnfreezeWallet() {

        // Arrange
        String phoneNumber = "9800000000";

        User user = new User();
        user.setId(1L);

        Wallet wallet = new Wallet();
        wallet.setId(10L);
        wallet.setUser(user);
        wallet.setStatus(WalletStatus.FROZEN);

        when(walletRepository.findByUserPhoneNumber(phoneNumber))
                .thenReturn(Optional.of(wallet));

        when(walletRepository.save(wallet))
                .thenReturn(wallet);

        // Act
        WalletDto result =
                adminService.unfreezeWalletByPhoneNumber(phoneNumber);

        // Assert
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());

        verify(walletRepository).save(wallet);

        verify(auditLogService).logEvent(
                eq(1L),
                eq("ADMIN_UNFREEZE_WALLET"),
                eq("Admin unfroze wallet for phone number: " + phoneNumber),
                eq(httpServletRequest)
        );
    }
    @Test
    void unfreezeWalletByPhoneNumber_shouldThrowException_whenWalletNotFound() {

        // Arrange
        String phoneNumber = "9800000000";

        when(walletRepository.findByUserPhoneNumber(phoneNumber))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> adminService.unfreezeWalletByPhoneNumber(phoneNumber)
                );

        assertEquals(
                "Wallet not found for phone number: " + phoneNumber,
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }
    @Test
    void unfreezeWalletByPhoneNumber_shouldThrowException_whenWalletAlreadyActive() {

        // Arrange
        String phoneNumber = "9800000000";

        Wallet wallet = new Wallet();
        wallet.setStatus(WalletStatus.ACTIVE);

        when(walletRepository.findByUserPhoneNumber(phoneNumber))
                .thenReturn(Optional.of(wallet));

        // Act & Assert
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> adminService.unfreezeWalletByPhoneNumber(phoneNumber)
                );

        assertEquals(
                "Wallet is already active.",
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }
    @Test
    void getAllUsers_shouldReturnUsers() {

        // Arrange
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPhoneNumber("9800000000");

        Page<User> userPage =
                new PageImpl<>(
                        List.of(user),
                        PageRequest.of(0, 10),
                        1
                );

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(userPage);

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        // Act
        Page<AdminUserResponseDto> result =
                adminService.getAllUsers(0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        AdminUserResponseDto dto =
                result.getContent().get(0);

        assertEquals(1L, dto.getId());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("9800000000", dto.getPhoneNumber());

        verify(userRepository).findAll(any(Pageable.class));
    }
    @Test
    void searchUsers_shouldReturnMatchingUsers() {

        // Arrange
        String query = "John";

        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPhoneNumber("9800000000");

        Page<User> userPage =
                new PageImpl<>(
                        List.of(user),
                        PageRequest.of(0, 10),
                        1
                );

        when(userRepository.searchUsers(
                eq(query),
                any(Pageable.class)
        )).thenReturn(userPage);

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        // Act
        Page<AdminUserResponseDto> result =
                adminService.searchUsers(query, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        assertEquals(
                "John Doe",
                result.getContent().get(0).getFullName()
        );

        verify(userRepository).searchUsers(
                eq(query),
                any(Pageable.class)
        );
    }
    @Test
    void resetUserTransactionPin_shouldResetPin() {

        // Arrange
        String phoneNumber = "9800000000";
        String newPin = "1234";
        String encodedPin = "encoded-pin";

        AdminResetPinRequest request =
                new AdminResetPinRequest();

        request.setPhoneNumber(phoneNumber);
        request.setNewPin(newPin);

        User user = new User();
        user.setId(1L);
        user.setPhoneNumber(phoneNumber);

        when(userRepository.findByPhoneNumber(phoneNumber))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode(newPin))
                .thenReturn(encodedPin);

        // Act
        adminService.resetUserTransactionPin(request);

        // Assert
        assertEquals(encodedPin, user.getTransactionPin());

        verify(passwordEncoder).encode(newPin);
        verify(userRepository).save(user);

        verify(auditLogService).logEvent(
                eq(1L),
                eq("ADMIN_PIN_RESET"),
                contains(phoneNumber),
                eq(httpServletRequest)
        );

        verify(eventPublisher).publishEvent(
                any(WalletTransactionEvent.class)
        );
    }
    @Test
    void resetUserTransactionPin_shouldThrowException_whenUserNotFound() {

        // Arrange
        String phoneNumber = "9800000000";

        AdminResetPinRequest request =
                new AdminResetPinRequest();

        request.setPhoneNumber(phoneNumber);
        request.setNewPin("1234");

        when(userRepository.findByPhoneNumber(phoneNumber))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> adminService.resetUserTransactionPin(request)
                );

        assertEquals(
                "User not found with phone number: " + phoneNumber,
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(eventPublisher);
    }
    @Test
    void changeUserPassword_shouldChangePassword() {

        // Arrange
        String phoneNumber = "9800000000";
        String newPassword = "newPassword";
        String encodedPassword = "encodedPassword";

        AdminChangePasswordRequest request =
                new AdminChangePasswordRequest();

        request.setPhoneNumber(phoneNumber);
        request.setNewPassword(newPassword);

        User user = new User();
        user.setId(1L);
        user.setPhoneNumber(phoneNumber);

        when(userRepository.findByPhoneNumber(phoneNumber))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode(newPassword))
                .thenReturn(encodedPassword);

        // Act
        adminService.changeUserPassword(request);

        // Assert
        assertEquals(
                encodedPassword,
                user.getPassword()
        );

        verify(passwordEncoder).encode(newPassword);

        verify(userRepository).save(user);

        verify(auditLogService).logEvent(
                eq(1L),
                eq("ADMIN_PASSWORD_CHANGE"),
                contains(phoneNumber),
                eq(httpServletRequest)
        );

        verify(eventPublisher).publishEvent(
                any(WalletTransactionEvent.class)
        );
    }
    @Test
    void changeUserPassword_shouldThrowException_whenUserNotFound() {

        // Arrange
        String phoneNumber = "9800000000";

        AdminChangePasswordRequest request =
                new AdminChangePasswordRequest();

        request.setPhoneNumber(phoneNumber);
        request.setNewPassword("newPassword");

        when(userRepository.findByPhoneNumber(phoneNumber))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> adminService.changeUserPassword(request)
                );

        assertEquals(
                "User not found with phone number: " + phoneNumber,
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(eventPublisher);
    }

}