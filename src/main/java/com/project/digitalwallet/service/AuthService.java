package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

public interface AuthService {
    void initiateRegistration(RegisterRequest request);

    @Transactional
    UserDto completeRegistration(
            RegisterVerifyRequest verifyRequest
    );

    public LoginResponse login(LoginRequest request);


    void initiateForgotPassword(ForgotPasswordRequest request);
    VerifyResetOtpResponse verifyResetOtp(VerifyResetOtpRequest request);
    void resetPasswordWithToken(ResetPasswordWithTokenRequest request);

    void logout(HttpServletRequest request);

}
