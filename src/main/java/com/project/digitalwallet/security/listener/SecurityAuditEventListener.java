package com.project.digitalwallet.security.listener;

import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityAuditEventListener {

    private final AuditLogService auditLogService;
    private final HttpServletRequest request;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof UserPrincipal userPrincipal) {
            auditLogService.logEvent(
                    userPrincipal.getUser().getId(),
                    "LOGIN_SUCCESS",
                    "User logged in successfully.",
                    request
            );
        }
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        auditLogService.logEvent(
                null,
                "LOGIN_FAILED",
                "Failed login attempt: Bad credentials for username " + event.getAuthentication().getPrincipal(),
                request
        );
    }
}