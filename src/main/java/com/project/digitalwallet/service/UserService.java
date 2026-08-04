package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.RegisterRequest;
import com.project.digitalwallet.dto.RegisterVerifyRequest;
import com.project.digitalwallet.dto.UserDto;

import java.util.List;

public interface UserService {

    void initiateRegistration(RegisterRequest request);

    UserDto completeRegistration(RegisterVerifyRequest request);

    List<UserDto> getAllUsers();
}
