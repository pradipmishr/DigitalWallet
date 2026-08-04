package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.RegisterRequest;
import com.project.digitalwallet.dto.RegisterVerifyRequest;
import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseWrapper<String> initiateRegistration(@Valid @RequestBody RegisterRequest registerRequest) {
        userService.initiateRegistration(registerRequest);
        return new ResponseWrapper<>(
                "OTP sent to " + registerRequest.getPhoneNumber(),
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
}