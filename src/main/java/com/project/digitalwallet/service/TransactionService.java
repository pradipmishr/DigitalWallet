package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.StatementRequest;
import com.project.digitalwallet.entity.Wallet;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

public interface TransactionService {
    ByteArrayInputStream generateCsvStatement(Long userId, StatementRequest request);
    public byte[] generatePdfStatement(Long userId, StatementRequest request);

}
