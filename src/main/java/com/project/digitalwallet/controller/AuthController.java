package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.service.AuthService;
import com.project.digitalwallet.service.OtpService;
import com.project.digitalwallet.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseWrapper<String> initiateRegistration(@Valid @RequestBody RegisterRequest registerRequest) {
        userService.initiateRegistration(registerRequest);
        return new ResponseWrapper<>(
                "OTP sent to " + registerRequest.getEmail(),
                "OTP sent successfully",
                HttpStatus.OK.value(),
                true
        );
    }

    @PostMapping("/register/verify")
    public ResponseWrapper<UserDto> completeRegistration(@Valid @RequestBody RegisterVerifyRequest verifyRequest) {
        UserDto response = userService.completeRegistration(verifyRequest);
        return new ResponseWrapper<>(
                response,
                "User created successfully and wallet initialized",
                HttpStatus.CREATED.value(),
                true
        );
    }
    @PostMapping("/login")
    public ResponseWrapper<LoginResponse> login(@RequestBody LoginRequest loginRequest){
        return new ResponseWrapper<>(authService.login(loginRequest),"Login Successful",HttpStatus.OK.value(), true);
    }
    @PostMapping("/resend-otp")
    public ResponseWrapper<String> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        otpService.resendOtp(request.getEmail());
        return new ResponseWrapper<>(
                "OTP resent successfully to " + request.getEmail(),
                "OTP resent successfully",
                HttpStatus.OK.value(),
                true
        );
    }
    @PostMapping("/forgot-password")
    public ResponseWrapper<String> initiateForgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.initiateForgotPassword(request);
        return new ResponseWrapper<>(
                "If an account exists with that email, an OTP has been sent.",
                "OTP sent successfully",
                HttpStatus.OK.value(),
                true
        );
    }

    // 2. Verify OTP & Receive Reset Token
    @PostMapping("/verify-reset-otp")
    public ResponseWrapper<VerifyResetOtpResponse> verifyResetOtp(@Valid @RequestBody VerifyResetOtpRequest request) {
        VerifyResetOtpResponse response = authService.verifyResetOtp(request);
        return new ResponseWrapper<>(
                response,
                "OTP verified successfully",
                HttpStatus.OK.value(),
                true
        );
    }

    // 3. Reset Password using Reset Token
    @PostMapping("/reset-password")
    public ResponseWrapper<String> resetPassword(@Valid @RequestBody ResetPasswordWithTokenRequest request) {
        authService.resetPasswordWithToken(request);
        return new ResponseWrapper<>(
                "Password reset successfully. You can now log in with your new password.",
                "SUCCESS",
                HttpStatus.OK.value(),
                true
        );
    }

    @PostMapping("/logout")
    public ResponseWrapper<?> logout(HttpServletRequest request) {

        authService.logout(request);

        return new ResponseWrapper<>(null,"Logout success",HttpStatus.OK.value(),true);
    }
}


