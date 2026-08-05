package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.RegisterRequest;
import com.project.digitalwallet.dto.SetTransactionPinRequest;
import com.project.digitalwallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @PostMapping("/transaction-pin")
    public ResponseWrapper<String> setTransactionPin(
            @RequestBody SetTransactionPinRequest request,
            Authentication authentication
    ) {

        String phoneNumber = authentication.getName();

        userService.setTransactionPin(
                phoneNumber,
                request.getPin()
        );

        return new ResponseWrapper<>(null,"Transaction PIN set successfully",HttpStatus.CREATED.value(),true);
    }

}
