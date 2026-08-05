package com.project.digitalwallet.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditLogService {
    void logEvent(Long userId, String action, String description, HttpServletRequest request);
}