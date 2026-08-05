package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.LoginResponse;
import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.entity.User;

import java.util.List;

public class UserMapper {
    public static UserDto toUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setEmail(user.getEmail());
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
        user.setWallet(WalletMapper.toWalletEntity(userDto.getWallet()));
        user.setPhoneNumber(userDto.getPhoneNumber());
        return user;
        }
    public static List<User> toUserEntity(List<UserDto> userDto){
        return userDto.stream()
                .map(UserMapper::toUserEntity)
                .toList();
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
