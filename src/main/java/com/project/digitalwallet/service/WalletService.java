package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.Wallet;

public interface WalletService {
    public WalletDto createWallet(UserDto userDto);
}
