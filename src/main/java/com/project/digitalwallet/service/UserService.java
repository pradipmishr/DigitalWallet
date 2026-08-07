package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.User;
import jakarta.transaction.Transactional;

import java.util.List;

public interface UserService {

    void initiateRegistration(RegisterRequest request);

    UserDto completeRegistration(RegisterVerifyRequest request);

    List<UserDto> getAllUsers();
    void setTransactionPin(String phoneNumber, String pin);


    void initiatePinReset(UserDto userDto);
    VerifyPinOtpResponse verifyPinOtp(UserDto userDto, VerifyPinOtpRequest request);
    void resetPinWithToken(UserDto userDto, ResetPinWithTokenRequest request);
}
