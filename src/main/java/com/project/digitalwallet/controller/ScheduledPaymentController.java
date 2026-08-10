package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.CancelScheduledPaymentRequest;
import com.project.digitalwallet.dto.CreateScheduledPaymentRequest;
import com.project.digitalwallet.dto.ScheduledPaymentDto;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.ScheduledPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scheduled-payments")
@RequiredArgsConstructor
public class ScheduledPaymentController {

    private final ScheduledPaymentService scheduledPaymentService;

    @PostMapping
    public ResponseEntity<ResponseWrapper<ScheduledPaymentDto>> createSchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateScheduledPaymentRequest request) {

        ScheduledPaymentDto result = scheduledPaymentService.createSchedule(userPrincipal.getUser().getId(), request);

        ResponseWrapper<ScheduledPaymentDto> response = new ResponseWrapper<>(
                result,
                "Scheduled payment created successfully",
                HttpStatus.CREATED.value(),
                true
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ResponseWrapper<List<ScheduledPaymentDto>>> getUserSchedules(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ScheduledPaymentDto> schedules = scheduledPaymentService.getUserSchedules(userPrincipal.getUser().getId());

        ResponseWrapper<List<ScheduledPaymentDto>> response = new ResponseWrapper<>(
                schedules,
                "User scheduled payments retrieved successfully",
                HttpStatus.OK.value(),
                true
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ResponseWrapper<Void>> cancelSchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody CancelScheduledPaymentRequest request) {

        scheduledPaymentService.cancelSchedule(userPrincipal.getUser().getId(), id, request);

        ResponseWrapper<Void> response = new ResponseWrapper<>(
                null,
                "Scheduled payment cancelled successfully",
                HttpStatus.OK.value(),
                true
        );

        return ResponseEntity.ok(response);
    }
}