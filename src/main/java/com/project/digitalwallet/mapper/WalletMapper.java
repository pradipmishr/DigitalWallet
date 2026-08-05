package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.Wallet;

public class WalletMapper {
//    public static WalletDto toWalletDto(Wallet wallet){
//        if (wallet == null) {
//            return null; // Return null safely if user doesn't have a wallet yet
//        }
//        WalletDto walletDto = new WalletDto();
//        walletDto.setId(wallet.getId());
//        walletDto.setBalance(wallet.getBalance());
//        walletDto.setStatus(wallet.getStatus());
//        return walletDto;
//    }
    public static WalletDto toWalletDto(Wallet wallet) {
        if (wallet == null) {
            return null;
        }

        WalletDto dto = new WalletDto();
        dto.setId(wallet.getId());
        dto.setWalletNumber(wallet.getWalletNumber());
        dto.setBalance(wallet.getBalance());
        dto.setStatus(wallet.getStatus());

        if (wallet.getUser() != null) {
            var user = wallet.getUser();
            dto.setUserId(user.getId());
            dto.setUserName(user.getFirstName() + " " + user.getLastName());
            dto.setUserPhoneNumber(user.getPhoneNumber());
        }

        return dto;
    }

    public static Wallet toWalletEntity(WalletDto walletDto){
        if (walletDto == null) {
            return null;
        }
        Wallet wallet = new Wallet();
        wallet.setId(walletDto.getId());
        wallet.setBalance(walletDto.getBalance());
        wallet.setStatus(walletDto.getStatus());
        return wallet;
    }
}
