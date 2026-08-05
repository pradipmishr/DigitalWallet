package com.project.digitalwallet.mapper;

import com.project.digitalwallet.common.enums.TransactionStatus;
import com.project.digitalwallet.common.enums.TransactionType;
import com.project.digitalwallet.dto.TransactionDto;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class TransactionMapper {

    public static TransactionDto toTransactionDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionDto dto = new TransactionDto();
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());
        dto.setDescription(transaction.getDescription());
        dto.setCreatedAt(transaction.getCreatedAt());

        // Map Sender Details
        if (transaction.getSenderWallet() != null && transaction.getSenderWallet().getUser() != null) {
            var senderUser = transaction.getSenderWallet().getUser();
            dto.setSenderName(senderUser.getFirstName() + " " + senderUser.getLastName());
            dto.setSenderPhoneNumber(senderUser.getPhoneNumber());
        }

        // Map Receiver Details
        if (transaction.getReceiverWallet() != null && transaction.getReceiverWallet().getUser() != null) {
            var receiverUser = transaction.getReceiverWallet().getUser();
            dto.setReceiverName(receiverUser.getFirstName() + " " + receiverUser.getLastName());
            dto.setReceiverPhoneNumber(receiverUser.getPhoneNumber());
        }

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
    public static Transaction createDepositEntity(Wallet wallet, BigDecimal amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setSenderWallet(null);
        transaction.setReceiverWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setReferenceNumber("DEP-" + generateRef());
        transaction.setDescription(description != null && !description.isBlank() ? description : "Self Deposit");
        return transaction;
    }

    public static Transaction createTransferEntity(Wallet senderWallet, Wallet receiverWallet, BigDecimal amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(receiverWallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setReferenceNumber("TRF-" + generateRef());
        transaction.setDescription(description != null && !description.isBlank() ? description : "P2P Transfer");
        return transaction;
    }

    private static String generateRef() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}