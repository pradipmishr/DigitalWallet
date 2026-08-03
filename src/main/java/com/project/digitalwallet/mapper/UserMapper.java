package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.entity.User;

public class UserMapper {
    public static UserDto toUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setEmail(user.getEmail());
        //userDto.setWallet(user.getWallet());
        userDto.setPhoneNumber(user.getPhoneNumber());
        return userDto;
    }

    public static User toUserEntity(UserDto userDto){
        User user = new User();
        user.setId(userDto.getId());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        //user.setWallet(userDto.getWallet());
        user.setPhoneNumber(userDto.getPhoneNumber());
        return user;
    }
}
