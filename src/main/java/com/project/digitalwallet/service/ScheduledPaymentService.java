package com.project.digitalwallet.service;


import com.project.digitalwallet.dto.ScheduledPaymentDto;
import com.project.digitalwallet.dto.CreateScheduledPaymentRequest;

import java.util.List;

public interface ScheduledPaymentService {
    ScheduledPaymentDto createSchedule(Long userId, CreateScheduledPaymentRequest request);
    List<ScheduledPaymentDto> getUserSchedules(Long userId);
    void cancelSchedule(Long userId, Long scheduleId);
}
