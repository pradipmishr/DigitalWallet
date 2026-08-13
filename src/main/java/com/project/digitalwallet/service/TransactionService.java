package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

public interface TransactionService {
    ByteArrayInputStream generateCsvStatement(Long userId, StatementRequest request);
    byte[] generatePdfStatement(Long userId, StatementRequest request);

    Page<TransactionDto> searchTransactionsForAdmin(AdminTransactionSearchRequest request);

    @Transactional
    TransactionDto reverseTransaction(UserDto adminUserDto, AdminReverseTransactionRequest request);
}
