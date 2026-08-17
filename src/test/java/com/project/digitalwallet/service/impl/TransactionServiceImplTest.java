package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.enums.TransactionStatus;
import com.project.digitalwallet.common.enums.TransactionType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void generateCsvStatement_shouldGenerateCsvForSender() {

        // Arrange
        Long userId = 1L;

        StatementRequest request = new StatementRequest();
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setEndDate(LocalDate.of(2026, 8, 10));

        User user = new User();
        user.setId(userId);

        Wallet senderWallet = new Wallet();
        senderWallet.setUser(user);

        Transaction transaction = new Transaction();
        transaction.setId(100L);
        transaction.setCreatedAt(
                LocalDateTime.of(2026, 8, 5, 14, 30)
        );
        transaction.setType(TransactionType.TRANSFER);
        transaction.setAmount(new BigDecimal("500.00"));
        transaction.setReferenceNumber("TXN-123");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setSenderWallet(senderWallet);

        when(transactionRepository.findByUserIdAndCreatedAtBetween(
                eq(userId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(transaction));

        // Act
        ByteArrayInputStream result =
                transactionService.generateCsvStatement(userId, request);

        // Convert stream to String
        String csv = new String(
                result.readAllBytes(),
                StandardCharsets.UTF_8
        );

        // Assert
        assertNotNull(result);

        assertTrue(csv.contains(
                "Transaction ID,Date & Time,Type,Amount,Fee,Reference,Status"
        ));

        assertTrue(csv.contains("100"));
        assertTrue(csv.contains("TRANSFER"));
        assertTrue(csv.contains("DEBIT"));
        assertTrue(csv.contains("500.00"));
        assertTrue(csv.contains("TXN-123"));
        assertTrue(csv.contains("SUCCESS"));

        verify(transactionRepository)
                .findByUserIdAndCreatedAtBetween(
                        eq(userId),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }
    @Test
    void generateCsvStatement_shouldMarkTransactionAsCreditForReceiver() {

        // Arrange
        Long userId = 2L;

        StatementRequest request = new StatementRequest();
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setEndDate(LocalDate.of(2026, 8, 10));

        User senderUser = new User();
        senderUser.setId(1L);

        Wallet senderWallet = new Wallet();
        senderWallet.setUser(senderUser);

        Transaction transaction = new Transaction();
        transaction.setId(101L);
        transaction.setCreatedAt(
                LocalDateTime.of(2026, 8, 5, 10, 0)
        );
        transaction.setType(TransactionType.TRANSFER);
        transaction.setAmount(new BigDecimal("250.00"));
        transaction.setReferenceNumber("TXN-456");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setSenderWallet(senderWallet);

        when(transactionRepository.findByUserIdAndCreatedAtBetween(
                eq(userId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(transaction));

        // Act
        ByteArrayInputStream result =
                transactionService.generateCsvStatement(userId, request);

        String csv = new String(
                result.readAllBytes(),
                StandardCharsets.UTF_8
        );

        // Assert
        assertTrue(csv.contains("CREDIT"));
        assertFalse(csv.contains("DEBIT"));

        verify(transactionRepository)
                .findByUserIdAndCreatedAtBetween(
                        eq(userId),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }
    @Test
    void generateCsvStatement_shouldReturnHeaderWhenNoTransactions() {

        // Arrange
        Long userId = 1L;

        StatementRequest request = new StatementRequest();
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setEndDate(LocalDate.of(2026, 8, 10));

        when(transactionRepository.findByUserIdAndCreatedAtBetween(
                eq(userId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        // Act
        ByteArrayInputStream result =
                transactionService.generateCsvStatement(userId, request);

        String csv = new String(
                result.readAllBytes(),
                StandardCharsets.UTF_8
        );

        // Assert
        assertNotNull(result);

        assertTrue(csv.contains(
                "Transaction ID,Date & Time,Type,Amount,Fee,Reference,Status"
        ));

        verify(transactionRepository)
                .findByUserIdAndCreatedAtBetween(
                        eq(userId),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }
    @Test
    void generatePdfStatement_shouldThrowException_whenUserNotFound() {

        // Arrange
        Long userId = 99L;

        StatementRequest request = new StatementRequest();
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setEndDate(LocalDate.of(2026, 8, 10));

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.generatePdfStatement(
                                userId,
                                request
                        )
                );

        assertEquals(
                "User not found: " + userId,
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByUserIdAndCreatedAtBetween(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }
    @Test
    void generatePdfStatement_shouldReturnBytes_whenUserExists() {

        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");

        StatementRequest request = new StatementRequest();
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setEndDate(LocalDate.of(2026, 8, 10));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(transactionRepository.findByUserIdAndCreatedAtBetween(
                eq(userId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        // Act
        byte[] result =
                transactionService.generatePdfStatement(userId, request);

        // Assert
        assertNotNull(result);

        verify(userRepository).findById(userId);

        verify(transactionRepository)
                .findByUserIdAndCreatedAtBetween(
                        eq(userId),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }
    @Test
    void searchTransactionsForAdmin_shouldReturnTransactions() {

        // Arrange
        AdminTransactionSearchRequest request =
                new AdminTransactionSearchRequest();

        request.setPage(0);
        request.setSize(10);

        Page<Transaction> transactionPage =
                new PageImpl<>(
                        List.of(),
                        PageRequest.of(0, 10),
                        0
                );

        when(transactionRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(transactionPage);

        // Act
        Page<TransactionDto> result =
                transactionService.searchTransactionsForAdmin(request);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());

        verify(transactionRepository)
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }
    @Test
    void searchTransactionsForAdmin_shouldAcceptReferenceNumber() {

        // Arrange
        AdminTransactionSearchRequest request =
                new AdminTransactionSearchRequest();

        request.setReferenceNumber(" TXN-123 ");
        request.setPage(0);
        request.setSize(10);

        when(transactionRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of())
        );

        // Act
        Page<TransactionDto> result =
                transactionService.searchTransactionsForAdmin(request);

        // Assert
        assertNotNull(result);

        verify(transactionRepository)
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }
    @Test
    void searchTransactionsForAdmin_shouldHandleAllFilters() {

        // Arrange
        AdminTransactionSearchRequest request =
                new AdminTransactionSearchRequest();

        request.setReferenceNumber("TXN-123");
        request.setSenderPhoneNumber("9800000000");
        request.setReceiverPhoneNumber("9811111111");
        request.setDate(LocalDate.of(2026, 8, 10));
        request.setPage(1);
        request.setSize(20);

        when(transactionRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of())
        );

        // Act
        Page<TransactionDto> result =
                transactionService.searchTransactionsForAdmin(request);

        // Assert
        assertNotNull(result);

        verify(transactionRepository)
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }
    @Test
    void reverseTransaction_shouldThrowException_whenTransactionNotFound() {

        // Arrange
        AdminReverseTransactionRequest request =
                new AdminReverseTransactionRequest();

        request.setReferenceNumber("TXN-999");
        request.setReason("Duplicate transaction");

        UserDto adminUser = new UserDto();
        adminUser.setId(100L);

        when(transactionRepository.findByReferenceNumber("TXN-999"))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.reverseTransaction(
                                adminUser,
                                request
                        )
                );

        assertEquals(
                "Original transaction not found with reference number: TXN-999",
                exception.getMessage()
        );

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(walletRepository);
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(eventPublisher);
    }
    @Test
    void reverseTransaction_shouldThrowException_whenAlreadyReversed() {

        // Arrange
        String reference = "TXN-123";

        AdminReverseTransactionRequest request =
                new AdminReverseTransactionRequest();

        request.setReferenceNumber(reference);
        request.setReason("Duplicate transaction");

        UserDto adminUser = new UserDto();
        adminUser.setId(100L);

        Transaction transaction = new Transaction();
        transaction.setStatus(TransactionStatus.REVERSED);

        when(transactionRepository.findByReferenceNumber(reference))
                .thenReturn(Optional.of(transaction));

        // Act & Assert
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> transactionService.reverseTransaction(
                                adminUser,
                                request
                        )
                );

        assertEquals(
                "This transaction has already been reversed.",
                exception.getMessage()
        );

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(walletRepository);
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(eventPublisher);
    }
    @Test
    void reverseTransaction_shouldThrowException_whenTransactionNotSuccessful() {

        // Arrange
        String reference = "TXN-123";

        AdminReverseTransactionRequest request =
                new AdminReverseTransactionRequest();

        request.setReferenceNumber(reference);
        request.setReason("Test reversal");

        UserDto adminUser = new UserDto();
        adminUser.setId(100L);

        Transaction transaction = new Transaction();
        transaction.setStatus(TransactionStatus.PENDING);

        when(transactionRepository.findByReferenceNumber(reference))
                .thenReturn(Optional.of(transaction));

        // Act & Assert
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> transactionService.reverseTransaction(
                                adminUser,
                                request
                        )
                );

        assertEquals(
                "Only SUCCESSFUL transactions can be reversed.",
                exception.getMessage()
        );

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(walletRepository);
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(eventPublisher);
    }
    @Test
    void reverseTransaction_shouldThrowException_whenReceiverHasInsufficientBalance() {

        // Arrange
        String reference = "TXN-123";

        AdminReverseTransactionRequest request =
                new AdminReverseTransactionRequest();

        request.setReferenceNumber(reference);
        request.setReason("Test reversal");

        UserDto adminUser = new UserDto();
        adminUser.setId(100L);

        User senderUser = new User();
        senderUser.setId(1L);

        User receiverUser = new User();
        receiverUser.setId(2L);

        Wallet senderWallet = new Wallet();
        senderWallet.setBalance(new BigDecimal("1000"));
        senderWallet.setUser(senderUser);

        Wallet receiverWallet = new Wallet();
        receiverWallet.setBalance(new BigDecimal("50"));
        receiverWallet.setUser(receiverUser);

        Transaction transaction = new Transaction();
        transaction.setReferenceNumber(reference);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setAmount(new BigDecimal("100"));
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(receiverWallet);

        when(transactionRepository.findByReferenceNumber(reference))
                .thenReturn(Optional.of(transaction));

        // Act & Assert
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> transactionService.reverseTransaction(
                                adminUser,
                                request
                        )
                );

        assertEquals(
                "Receiver wallet has insufficient funds to process reversal.",
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(eventPublisher);
    }
    @Test
    void reverseTransaction_shouldSuccessfullyReverseTransaction() {

        // Arrange
        String reference = "TXN-123";
        BigDecimal amount = new BigDecimal("100");

        AdminReverseTransactionRequest request =
                new AdminReverseTransactionRequest();

        request.setReferenceNumber(reference);
        request.setReason("Duplicate transaction");

        UserDto adminUser = new UserDto();
        adminUser.setId(999L);

        // Sender
        User senderUser = new User();
        senderUser.setId(1L);
        senderUser.setPhoneNumber("9800000000");

        Wallet senderWallet = new Wallet();
        senderWallet.setUser(senderUser);
        senderWallet.setBalance(new BigDecimal("500"));

        // Receiver
        User receiverUser = new User();
        receiverUser.setId(2L);
        receiverUser.setPhoneNumber("9811111111");

        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiverUser);
        receiverWallet.setBalance(new BigDecimal("500"));

        // Original transaction
        Transaction originalTx = new Transaction();
        originalTx.setId(10L);
        originalTx.setReferenceNumber(reference);
        originalTx.setStatus(TransactionStatus.SUCCESS);
        originalTx.setAmount(amount);
        originalTx.setSenderWallet(senderWallet);
        originalTx.setReceiverWallet(receiverWallet);

        when(transactionRepository.findByReferenceNumber(reference))
                .thenReturn(Optional.of(originalTx));

        // walletRepository.save() returns the same wallet
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // transactionRepository.save()
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {

                    Transaction tx = invocation.getArgument(0);

                    // Give the new reversal transaction an ID
                    if (tx.getId() == null) {
                        tx.setId(20L);
                    }

                    return tx;
                });

        // Act
        TransactionDto result =
                transactionService.reverseTransaction(
                        adminUser,
                        request
                );

        // Assert
        assertNotNull(result);

        // Sender balance: 500 + 100 = 600
        assertEquals(
                new BigDecimal("600"),
                senderWallet.getBalance()
        );

        // Receiver balance: 500 - 100 = 400
        assertEquals(
                new BigDecimal("400"),
                receiverWallet.getBalance()
        );

        // Original transaction should become REVERSED
        assertEquals(
                TransactionStatus.REVERSED,
                originalTx.getStatus()
        );

        // Verify wallets were saved
        verify(walletRepository).save(receiverWallet);
        verify(walletRepository).save(senderWallet);

        // Verify original transaction was saved
        verify(transactionRepository).save(originalTx);

        // Verify reversal transaction was also saved
        verify(transactionRepository, times(2))
                .save(any(Transaction.class));

        // Verify audit log
        verify(auditLogService).logEvent(
                eq(999L),
                eq("ADMIN_TRANSACTION_REVERSAL"),
                contains("TXN-123"),
                eq(httpServletRequest)
        );

        // Capture published events
        ArgumentCaptor<WalletTransactionEvent> eventCaptor =
                ArgumentCaptor.forClass(WalletTransactionEvent.class);

        verify(eventPublisher, times(2))
                .publishEvent(eventCaptor.capture());

        List<WalletTransactionEvent> events =
                eventCaptor.getAllValues();

        // Make sure two events were published
        assertEquals(2, events.size());

        // First event: sender receives refund
        assertEquals(
                NotificationType.TRANSACTION_CREDIT,
                events.get(0).type()
        );

        assertEquals(
                1L,
                events.get(0).userId()
        );

        assertEquals(
                "9800000000",
                events.get(0).userPhoneNumber()
        );

        assertEquals(
                amount,
                events.get(0).amount()
        );

        assertEquals(
                "NPR",
                events.get(0).currency()
        );

        assertEquals(
                "REV-TXN-123",
                events.get(0).referenceId()
        );

        // Second event: receiver gets debited
        assertEquals(
                NotificationType.TRANSACTION_DEBIT,
                events.get(1).type()
        );

        assertEquals(
                2L,
                events.get(1).userId()
        );

        assertEquals(
                "9811111111",
                events.get(1).userPhoneNumber()
        );

        assertEquals(
                amount,
                events.get(1).amount()
        );

        assertEquals(
                "NPR",
                events.get(1).currency()
        );

        assertEquals(
                "REV-TXN-123",
                events.get(1).referenceId()
        );
    }
    @Test
    void reverseTransaction_shouldNotPublishSenderEvent_whenSenderUserIsNull() {

        // Arrange
        String reference = "TXN-123";
        BigDecimal amount = new BigDecimal("100");

        AdminReverseTransactionRequest request =
                new AdminReverseTransactionRequest();

        request.setReferenceNumber(reference);
        request.setReason("Test");

        UserDto adminUser = new UserDto();
        adminUser.setId(999L);

        // Sender wallet has NO user
        Wallet senderWallet = new Wallet();
        senderWallet.setUser(null);
        senderWallet.setBalance(new BigDecimal("500"));

        // Receiver has a user
        User receiverUser = new User();
        receiverUser.setId(2L);
        receiverUser.setPhoneNumber("9811111111");

        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiverUser);
        receiverWallet.setBalance(new BigDecimal("500"));

        // Original transaction
        Transaction originalTx = new Transaction();
        originalTx.setReferenceNumber(reference);
        originalTx.setStatus(TransactionStatus.SUCCESS);
        originalTx.setAmount(amount);
        originalTx.setSenderWallet(senderWallet);
        originalTx.setReceiverWallet(receiverWallet);

        when(transactionRepository.findByReferenceNumber(reference))
                .thenReturn(Optional.of(originalTx));

        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionService.reverseTransaction(
                adminUser,
                request
        );

        // Assert

        // Only ONE event should be published.
        // Sender has no user, so sender event should NOT be published.
        verify(eventPublisher, times(1))
                .publishEvent(any(WalletTransactionEvent.class));

        // Capture the one event
        ArgumentCaptor<WalletTransactionEvent> eventCaptor =
                ArgumentCaptor.forClass(WalletTransactionEvent.class);

        verify(eventPublisher)
                .publishEvent(eventCaptor.capture());

        WalletTransactionEvent event =
                eventCaptor.getValue();

        // It must be the receiver's event
        assertEquals(
                2L,
                event.userId()
        );

        assertEquals(
                "9811111111",
                event.userPhoneNumber()
        );

        assertEquals(
                NotificationType.TRANSACTION_DEBIT,
                event.type()
        );

        assertEquals(
                amount,
                event.amount()
        );

        assertEquals(
                "NPR",
                event.currency()
        );

        assertEquals(
                "REV-TXN-123",
                event.referenceId()
        );

        // Sender event must NOT exist
        // Since there was only one interaction, this proves
        // that the sender event was not published.
        verify(eventPublisher, times(1))
                .publishEvent(any(WalletTransactionEvent.class));
    }

}