package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.AcceptMoneyRequest;
import com.project.digitalwallet.dto.CreateMoneyRequest;
import com.project.digitalwallet.dto.RequestMoneyDto;

import java.util.List;

public interface RequestMoneyService {
    RequestMoneyDto createRequest(Long requesterId, CreateMoneyRequest request);
    RequestMoneyDto acceptRequest(Long payerId, Long requestId, AcceptMoneyRequest request);
    void declineRequest(Long payerId, Long requestId);
    void cancelRequest(Long requesterId, Long requestId);
    List<RequestMoneyDto> getSentRequests(Long userId);
    List<RequestMoneyDto> getReceivedRequests(Long userId);
    void cleanupExpiredRequests();
}