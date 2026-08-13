package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.*;
import org.springframework.data.domain.Page;

public interface WalletService {
    WalletDto createWallet(UserDto userDto);

    WalletDto getCurrentUserWallet(Long userId);

    TransactionDto deposit(Long userId, DepositRequest request);

    TransactionDto transfer(Long senderUserId, TransferRequest request);

    Page<TransactionDto> getTransactionHistory(Long userId, int page, int size);
}
