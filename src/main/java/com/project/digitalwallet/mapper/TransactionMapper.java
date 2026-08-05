package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.TransactionDto;
import com.project.digitalwallet.entity.Transaction;

import java.util.List;

public class TransactionMapper {

    public static TransactionDto toTransactionDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setSenderWalletId(transaction.getSenderWallet() != null ? transaction.getSenderWallet().getId() : null);
        dto.setReceiverWalletId(transaction.getReceiverWallet() != null ? transaction.getReceiverWallet().getId() : null);
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());
        dto.setDescription(transaction.getDescription());
        dto.setCreatedAt(transaction.getCreatedAt());

        return dto;
    }

    public static List<TransactionDto> toTransactionDtoList(List<Transaction> transactions) {
        if (transactions == null) {
            return List.of();
        }
        return transactions.stream()
                .map(TransactionMapper::toTransactionDto)
                .toList();
    }
}