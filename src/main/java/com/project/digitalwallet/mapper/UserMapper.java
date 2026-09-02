package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.Address;
import com.project.digitalwallet.entity.KycDetails;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;

import java.util.List;
import java.util.Map;

public class UserMapper {
    public static UserDto toUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setEmail(user.getEmail());
        userDto.setDateOfBirth(user.getDateOfBirth());
        userDto.setWallet(WalletMapper.toWalletDto(user.getWallet()));
        userDto.setPhoneNumber(user.getPhoneNumber());
        return userDto;
    }
    public static List<UserDto> toUserDto(List<User> users){
        return users.stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    public static User toUserEntity(UserDto userDto){
        User user = new User();
        user.setId(userDto.getId());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setDateOfBirth(userDto.getDateOfBirth());
        user.setWallet(WalletMapper.toWalletEntity(userDto.getWallet()));
        user.setPhoneNumber(userDto.getPhoneNumber());
        return user;
        }
    public static List<User> toUserEntity(List<UserDto> userDto){
        return userDto.stream()
                .map(UserMapper::toUserEntity)
                .toList();
    }
//    public static UserByIdDto toUserByIdDto(User user, KycDetails kycDetails, Wallet wallet, List<TransactionDto> transactionDtos){
//        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
//                " " +
//                (user.getLastName() != null ? user.getLastName() : "");;
//        Address address = user.getAddress();
//        AddressDto addressDto = null;
//
//        if (address != null) {
//            addressDto = AddressDto.builder()
//                    .city(address.getCity())
//                    .state(address.getState())
//                    .zipCode(address.getZipCode())
//                    .street(address.getStreet())
//                    .build();
//        }
//        return UserByIdDto.builder()
//                .id(user.getId())
//                .fullName(fullName)
//                .kycStatus(kycDetails.getStatus())
//                .walletStatus(wallet.getStatus())
//                .email(user.getEmail())
//                .address(addressDto)
//                .dateOfBirth(user.getDateOfBirth())
//                .createdAt(user.getCreatedAt())
//                .phoneNumber(user.getPhoneNumber())
//                .transactions(transactionDtos)
//                .build();
//    }

    public static UserByIdDto toUserByIdDto(
            User user,
            KycDetails kycDetails,
            Wallet wallet,
            Map<String, List<TransactionDto>> transactions
    ) {

        String fullName =
                java.util.stream.Stream.of(
                                user.getFirstName(),
                                user.getLastName()
                        )
                        .filter(java.util.Objects::nonNull)
                        .filter(name -> !name.isBlank())
                        .collect(java.util.stream.Collectors.joining(" "));

        Address address = user.getAddress();

        AddressDto addressDto = null;

        if (address != null) {

            addressDto = AddressDto.builder()
                    .city(address.getCity())
                    .state(address.getState())
                    .zipCode(address.getZipCode())
                    .street(address.getStreet())
                    .build();
        }

        return UserByIdDto.builder()
                .id(user.getId())
                .fullName(fullName)
                .kycStatus(kycDetails.getStatus())
                .walletStatus(wallet.getStatus())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(addressDto)
                .dateOfBirth(user.getDateOfBirth())
                .createdAt(user.getCreatedAt())
                .transactions(transactions)
                .build();
    }


    public static LoginResponse toLoginResponse(User user, String token) {

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .build();
    }
}
