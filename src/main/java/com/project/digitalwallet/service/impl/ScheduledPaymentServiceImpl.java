package com.project.digitalwallet.service.impl;


import com.project.digitalwallet.common.enums.ScheduledPaymentStatus;
import com.project.digitalwallet.dto.ScheduledPaymentDto;
import com.project.digitalwallet.dto.CreateScheduledPaymentRequest;
import com.project.digitalwallet.entity.ScheduledPayment;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.ScheduleMapper;
import com.project.digitalwallet.repository.ScheduledPaymentRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.ScheduledPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.project.digitalwallet.mapper.ScheduleMapper.mapToDto;

@Service
@RequiredArgsConstructor
public class ScheduledPaymentServiceImpl implements ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ScheduledPaymentDto createSchedule(Long userId, CreateScheduledPaymentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Ensure recipient exists
        if (!userRepository.existsByPhoneNumber(request.getRecipientPhoneNumber())) {
            throw new IllegalArgumentException("Recipient phone number not registered: " + request.getRecipientPhoneNumber());
        }

        ScheduledPayment schedule = ScheduledPayment.builder()
                .user(user)
                .recipientPhoneNumber(request.getRecipientPhoneNumber())
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .status(ScheduledPaymentStatus.ACTIVE)
                .nextRunAt(request.getStartAt())
                .totalOccurrences(request.getTotalOccurrences())
                .completedOccurrences(0)
                .failedAttempts(0)
                .description(request.getDescription())
                .build();

        ScheduledPayment saved = scheduledPaymentRepository.save(schedule);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledPaymentDto> getUserSchedules(Long userId) {
        return scheduledPaymentRepository.findByUserId(userId)
                .stream()
                .map(ScheduleMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelSchedule(Long userId, Long scheduleId) {
        ScheduledPayment schedule = scheduledPaymentRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + scheduleId));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Unauthorized to cancel this schedule.");
        }

        schedule.setStatus(ScheduledPaymentStatus.CANCELLED);
        scheduledPaymentRepository.save(schedule);
    }


}
