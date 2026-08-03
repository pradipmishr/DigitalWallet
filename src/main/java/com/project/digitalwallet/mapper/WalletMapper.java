package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.Wallet;

public class WalletMapper {
    public static WalletDto toWalletDto(Wallet wallet){
        WalletDto walletDto = new WalletDto();
        walletDto.setId(wallet.getId());
        walletDto.setBalance(wallet.getBalance());
        walletDto.setStatus(wallet.getStatus());
        return walletDto;
    }

    public static Wallet toWalletEntity(WalletDto walletDto){
        Wallet wallet = new Wallet();
        wallet.setId(walletDto.getId());
        wallet.setBalance(walletDto.getBalance());
        wallet.setStatus(walletDto.getStatus());
        return wallet;
    }
}
