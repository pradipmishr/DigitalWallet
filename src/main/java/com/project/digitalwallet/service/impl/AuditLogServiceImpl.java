package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.entity.AuditLog;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.repository.AuditLogRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(Long userId, String action, String description, HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();

            if (userId != null) {
                // getReferenceById creates a lightweight proxy attached ONLY to this transaction
                User userRef = userRepository.getReferenceById(userId);
                auditLog.setUser(userRef);
            }

            auditLog.setAction(action);
            auditLog.setDescription(description);
            auditLog.setIpAddress(extractIpAddress(request));

            AuditLog saved = auditLogRepository.saveAndFlush(auditLog);
            log.info("Successfully persisted AuditLog ID: {} for action: {}", saved.getId(), action);
        } catch (Exception e) {
            log.error("Failed to save audit log for action: {}", action, e);
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0];
        }
        return ip != null ? ip : "UNKNOWN";
    }
}