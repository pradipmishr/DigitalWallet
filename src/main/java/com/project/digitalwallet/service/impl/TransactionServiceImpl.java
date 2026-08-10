package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.enums.TransactionStatus;
import com.project.digitalwallet.common.enums.TransactionType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.mapper.TransactionMapper;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.AuditLogService;
import com.project.digitalwallet.service.TransactionService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest httpServletRequest;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    public ByteArrayInputStream generateCsvStatement(Long userId, StatementRequest request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository.findByUserIdAndCreatedAtBetween(
                userId, startDateTime, endDateTime
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);

        // Header line
        printWriter.println("Transaction ID,Date & Time,Type,Amount,Fee,Reference,Status");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Transaction tx : transactions) {
            // Determine if the user was the sender (DEBIT) or receiver (CREDIT)
            boolean isSender = tx.getSenderWallet() != null && tx.getSenderWallet().getUser().getId().equals(userId);
            String direction = isSender ? "DEBIT" : "CREDIT";

            printWriter.printf(
                    "%d,%s,%s,%s,%.2f,%s,%s\n",
                    tx.getId(),
                    tx.getCreatedAt().format(formatter),
                    tx.getType(),
                    direction,
                    tx.getAmount(),
                    tx.getReferenceNumber() != null ? tx.getReferenceNumber() : "N/A",
                    tx.getStatus()
            );
        }

        printWriter.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public byte[] generatePdfStatement(Long userId, StatementRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository.findByUserIdAndCreatedAtBetween(
                userId, startDateTime, endDateTime
        );

        // HTML content string formatted for OpenHTMLtoPDF or Flying Saucer / iText
        String htmlContent = buildStatementHtml(user, transactions, request);

        // Convert HTML string to PDF ByteArrayOutputStream
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

        /*
         * Note: If using OpenHTMLToPDF library:
         * PdfRendererBuilder builder = new PdfRendererBuilder();
         * builder.withHtmlContent(htmlContent, "/");
         * builder.toStream(pdfOutputStream);
         * builder.run();
         */

        return pdfOutputStream.toByteArray();
    }

    private String buildStatementHtml(User user, List<Transaction> transactions, StatementRequest request) {
        StringBuilder tableRows = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Transaction tx : transactions) {
            tableRows.append(String.format("""
                <tr>
                    <td style="padding: 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px;">%s</td>
                    <td style="padding: 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px;">%s</td>
                    <td style="padding: 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px; font-weight: 600;">$%.2f</td>
                    <td style="padding: 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px;">%s</td>
                </tr>
                """,
                    tx.getCreatedAt().format(formatter),
                    tx.getType(),
                    tx.getAmount(),
                    tx.getStatus()
            ));
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><style>body { font-family: sans-serif; padding: 20px; }</style></head>
            <body>
                <h2>Account Statement</h2>
                <p><strong>Account Holder:</strong> %s %s</p>
                <p><strong>Period:</strong> %s to %s</p>
                <table style="width: 100%%; border-collapse: collapse; margin-top: 20px;">
                    <thead>
                        <tr style="background-color: #1e293b; color: white;">
                            <th style="padding: 10px; text-align: left;">Date</th>
                            <th style="padding: 10px; text-align: left;">Type</th>
                            <th style="padding: 10px; text-align: left;">Amount</th>
                            <th style="padding: 10px; text-align: left;">Status</th>
                        </tr>
                    </thead>
                    <tbody>%s</tbody>
                </table>
            </body>
            </html>
            """, user.getFirstName(), user.getLastName(), request.getStartDate(), request.getEndDate(), tableRows);
    }
    @Override
    public Page<TransactionDto> searchTransactionsForAdmin(AdminTransactionSearchRequest request) {
        Specification<Transaction> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Direct Reference Number Lookup (Highest Priority)
            if (request.getReferenceNumber() != null && !request.getReferenceNumber().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("referenceNumber"),
                        request.getReferenceNumber().trim()
                ));
            }

            // 2. Filter by Sender Phone Number
            if (request.getSenderPhoneNumber() != null && !request.getSenderPhoneNumber().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("senderWallet").get("user").get("phoneNumber"),
                        request.getSenderPhoneNumber().trim()
                ));
            }

            // 3. Filter by Receiver Phone Number
            if (request.getReceiverPhoneNumber() != null && !request.getReceiverPhoneNumber().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("receiverWallet").get("user").get("phoneNumber"),
                        request.getReceiverPhoneNumber().trim()
                ));
            }

            // 4. Filter by Date
            if (request.getDate() != null) {
                LocalDateTime startOfDay = request.getDate().atStartOfDay();
                LocalDateTime endOfDay = request.getDate().atTime(LocalTime.MAX);
                predicates.add(criteriaBuilder.between(root.get("createdAt"), startOfDay, endOfDay));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("createdAt").descending());
        return transactionRepository.findAll(spec, pageable).map(TransactionMapper::toTransactionDto);
    }
    @Override
    @Transactional
    public TransactionDto reverseTransaction(UserDto adminUserDto, AdminReverseTransactionRequest request) {
        Transaction originalTx = transactionRepository.findByReferenceNumber(request.getReferenceNumber())
                .orElseThrow(() -> new IllegalArgumentException("Original transaction not found with reference number: " + request.getReferenceNumber()));


        // 2. Ensure transaction is in SUCCESS state and hasn't been REVERSED yet
        if (originalTx.getStatus() == TransactionStatus.REVERSED) {
            throw new IllegalStateException("This transaction has already been reversed.");
        }
        if (originalTx.getStatus() != TransactionStatus.SUCCESS) {
            throw new IllegalStateException("Only SUCCESSFUL transactions can be reversed.");
        }

        Wallet senderWallet = originalTx.getSenderWallet();
        Wallet receiverWallet = originalTx.getReceiverWallet();
        BigDecimal amount = originalTx.getAmount();

        // 3. Ensure recipient has enough balance to claw back
        if (receiverWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Receiver wallet has insufficient funds to process reversal.");
        }

        // 4. Update balances (Debit receiver, Credit sender)
        receiverWallet.setBalance(receiverWallet.getBalance().subtract(amount));
        senderWallet.setBalance(senderWallet.getBalance().add(amount));

        walletRepository.save(receiverWallet);
        walletRepository.save(senderWallet);

        // 5. Mark original transaction status as REVERSED
        originalTx.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(originalTx);

        // 6. Create new REVERSAL entry in the ledger
        Transaction reversalTx = Transaction.builder()
                .senderWallet(receiverWallet)
                .receiverWallet(senderWallet)
                .amount(amount)
                .type(TransactionType.REVERSAL)
                .status(TransactionStatus.SUCCESS)
                .referenceNumber("REV-" + originalTx.getReferenceNumber())
                .description("REVERSAL for Tx #" + originalTx.getId() + ". Reason: " + request.getReason())
                .build();

        Transaction savedReversal = transactionRepository.save(reversalTx);

        // 7. Audit log
        auditLogService.logEvent(
                adminUserDto.getId(),
                "ADMIN_TRANSACTION_REVERSAL",
                String.format("Admin reversed Tx #%d (Amount: %s). Reason: %s", originalTx.getId(), amount, request.getReason()),
                httpServletRequest
        );
        if (senderWallet.getUser() != null) {
            eventPublisher.publishEvent(new WalletTransactionEvent(
                    senderWallet.getUser().getId(),
                    senderWallet.getUser().getPhoneNumber(),
                    NotificationType.TRANSACTION_CREDIT,
                    amount,
                    "NPR",
                    savedReversal.getReferenceNumber(),
                    String.format("Refund for reversed Tx #%s. Reason: %s",
                            originalTx.getReferenceNumber(), request.getReason())
            ));
        }

        if (receiverWallet.getUser() != null) {
            eventPublisher.publishEvent(new WalletTransactionEvent(
                    receiverWallet.getUser().getId(),
                    receiverWallet.getUser().getPhoneNumber(),
                    NotificationType.TRANSACTION_DEBIT,
                    amount,
                    "NPR",
                    savedReversal.getReferenceNumber(),
                    String.format("Clawback for reversed Tx #%s. Reason: %s",
                            originalTx.getReferenceNumber(), request.getReason())
            ));
        }

        return TransactionMapper.toTransactionDto(savedReversal);
    }
}