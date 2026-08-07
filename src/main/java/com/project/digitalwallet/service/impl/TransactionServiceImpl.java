package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.dto.StatementRequest;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

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
}