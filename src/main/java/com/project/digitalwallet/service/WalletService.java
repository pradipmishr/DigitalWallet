package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.Wallet;

public interface WalletService {
    public WalletDto createWallet(UserDto userDto);
    TransactionDto deposit(Long userId, DepositRequest request);
    TransactionDto transfer(Long senderUserId, TransferRequest request);
}
