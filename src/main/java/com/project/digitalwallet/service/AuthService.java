package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    public LoginResponse login(LoginRequest request);


    void initiateForgotPassword(ForgotPasswordRequest request);
    VerifyResetOtpResponse verifyResetOtp(VerifyResetOtpRequest request);
    void resetPasswordWithToken(ResetPasswordWithTokenRequest request);

    void logout(HttpServletRequest request);

}
