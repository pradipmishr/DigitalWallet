package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.AcceptMoneyRequest;
import com.project.digitalwallet.dto.CreateMoneyRequest;
import com.project.digitalwallet.dto.RequestMoneyDto;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.RequestMoneyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/request-money")
@RequiredArgsConstructor
public class RequestMoneyController {

    private final RequestMoneyService requestMoneyService;

    @PostMapping("/create")
    public ResponseEntity<ResponseWrapper<RequestMoneyDto>> createRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateMoneyRequest request) {

        RequestMoneyDto dto = requestMoneyService.createRequest(currentUser.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseWrapper<>(dto, "Money request sent successfully.", HttpStatus.CREATED.value(), true));
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<ResponseWrapper<RequestMoneyDto>> acceptRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long requestId,
            @Valid @RequestBody AcceptMoneyRequest request) {

        RequestMoneyDto dto = requestMoneyService.acceptRequest(currentUser.getUser().getId(), requestId, request);
        return ResponseEntity.ok(new ResponseWrapper<>(dto, "Money request accepted and processed.", HttpStatus.OK.value(), true));
    }

    @PostMapping("/{requestId}/decline")
    public ResponseEntity<ResponseWrapper<Void>> declineRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long requestId) {

        requestMoneyService.declineRequest(currentUser.getUser().getId(), requestId);
        return ResponseEntity.ok(new ResponseWrapper<>(null, "Money request declined.", HttpStatus.OK.value(), true));
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ResponseWrapper<Void>> cancelRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long requestId) {

        requestMoneyService.cancelRequest(currentUser.getUser().getId(), requestId);
        return ResponseEntity.ok(new ResponseWrapper<>(null, "Money request cancelled.", HttpStatus.OK.value(), true));
    }

    @GetMapping("/sent")
    public ResponseEntity<ResponseWrapper<List<RequestMoneyDto>>> getSentRequests(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<RequestMoneyDto> list = requestMoneyService.getSentRequests(currentUser.getUser().getId());
        return ResponseEntity.ok(new ResponseWrapper<>(list, "Sent money requests retrieved.", HttpStatus.OK.value(), true));
    }

    @GetMapping("/received")
    public ResponseEntity<ResponseWrapper<List<RequestMoneyDto>>> getReceivedRequests(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<RequestMoneyDto> list = requestMoneyService.getReceivedRequests(currentUser.getUser().getId());
        return ResponseEntity.ok(new ResponseWrapper<>(list, "Received money requests retrieved.", HttpStatus.OK.value(), true));
    }
}