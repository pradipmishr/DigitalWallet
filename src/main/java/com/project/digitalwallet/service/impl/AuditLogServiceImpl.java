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
import org.springframework.transaction.annotation.Transactional;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void logEvent(Long userId, String action, String description, HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();

            if (userId != null) {
                // getReferenceById creates a lightweight proxy attached ONLY to this transaction
                User userRef = userRepository.getReferenceById(userId);
                auditLog.setUser(userRef);
            }

            InetAddress local = null;
            InetAddress remote = null;
            try {
                // Get local machine IP
                local = Inet4Address.getLocalHost();
                System.out.println("Local IP: " + local.getHostAddress());

                // Get IP for a domain name
                remote = Inet6Address.getByName(local.getHostName());
                System.out.println("Remote IP: " + remote.getHostAddress());
            } catch (Exception e) {
                System.out.println("Error resolving IP");
            }

            auditLog.setAction(action);
            auditLog.setDescription(description);
            auditLog.setIpAddress(remote.getHostAddress());

            AuditLog saved = auditLogRepository.saveAndFlush(auditLog);
            log.info("Successfully persisted AuditLog ID: {} for action: {}", saved.getId(), action);
        } catch (Exception e) {
            log.error("Failed to save audit log for action: {}", action, e);
        }
    }


}