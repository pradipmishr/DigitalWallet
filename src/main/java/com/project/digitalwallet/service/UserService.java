package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.entity.User;

import java.util.List;

public interface UserService {
    UserDto createUser(UserDto userDto);
    List<UserDto> getAllUsers();

}
