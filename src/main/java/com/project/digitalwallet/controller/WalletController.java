package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.DepositRequest;
import com.project.digitalwallet.dto.TransactionDto;
import com.project.digitalwallet.dto.TransferRequest;
import com.project.digitalwallet.security.UserPrincipal; // Import your UserPrincipal
import com.project.digitalwallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deposit")
    public ResponseWrapper<TransactionDto> deposit(
            @AuthenticationPrincipal UserPrincipal currentUser, // Use UserPrincipal here
            @Valid @RequestBody DepositRequest request
    ) {
        if (currentUser == null) {
            throw new IllegalStateException("Unauthenticated user.");
        }

        // Access the underlying User entity or ID from UserPrincipal
        Long userId = currentUser.getUser().getId();

        TransactionDto response = walletService.deposit(userId, request);
        return new ResponseWrapper<>(
                response,
                "Funds deposited successfully",
                HttpStatus.OK.value(),
                true
        );
    }
    @PostMapping("/transfer")
    public ResponseWrapper<TransactionDto> transfer(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransferRequest request
    ) {
        if (currentUser == null) {
            throw new IllegalStateException("Unauthenticated user.");
        }

        Long senderUserId = currentUser.getUser().getId();
        TransactionDto response = walletService.transfer(senderUserId, request);

        return new ResponseWrapper<>(
                response,
                "Transfer successful",
                HttpStatus.OK.value(),
                true
        );
    }
}