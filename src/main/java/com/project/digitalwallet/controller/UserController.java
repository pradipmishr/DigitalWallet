package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.SetTransactionPinRequest;
import com.project.digitalwallet.dto.StatementRequest;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.TransactionService;
import com.project.digitalwallet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final TransactionService transactionService;

    @PostMapping("/transaction-pin")
    public ResponseWrapper<String> setTransactionPin(
            @RequestBody @Valid SetTransactionPinRequest request,
            Authentication authentication
    ) {

        String phoneNumber = authentication.getName();

        userService.setTransactionPin(
                phoneNumber,
                request.getPin()
        );

        return new ResponseWrapper<>(null,"Transaction PIN set successfully",HttpStatus.CREATED.value(),true);
    }

    @PostMapping("/statement/csv")
    public ResponseEntity<InputStreamResource> downloadCsvStatement(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody StatementRequest request) {
        Long userId = userPrincipal.getUser().getId();
        ByteArrayInputStream in = transactionService.generateCsvStatement(userId, request);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=statement_" + request.getStartDate() + "_to_" + request.getEndDate() + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(in));
    }
    @PostMapping("/statement/pdf")
    public ResponseEntity<ByteArrayResource> downloadPdfStatement(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody StatementRequest request) {
        Long userId = userPrincipal.getUser().getId();
        byte[] pdfBytes = transactionService.generatePdfStatement(userId, request);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=statement_" + request.getStartDate() + "_to_" + request.getEndDate() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(new ByteArrayResource(pdfBytes));
    }

}
