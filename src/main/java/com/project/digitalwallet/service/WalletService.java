package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.DepositRequest;
import com.project.digitalwallet.dto.TransactionDto;
import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.Wallet;

public interface WalletService {
    public WalletDto createWallet(UserDto userDto);
    TransactionDto deposit(Long userId, DepositRequest request);
}
