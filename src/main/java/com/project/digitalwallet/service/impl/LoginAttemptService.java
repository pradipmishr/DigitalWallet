package com.project.digitalwallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_TIME_MINUTES = 5;

    private static final String ATTEMPTS_PREFIX = "login_attempts:";
    private static final String LOCKOUT_PREFIX = "login_lockout:";

    /**
     * Checks if the phone number is currently locked out.
     */
    public boolean isBlocked(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false;
        }
        String lockoutKey = LOCKOUT_PREFIX + phoneNumber;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey));
    }

    /**
     * Increments failed attempt count and sets a 5-minute lock if threshold (3) is reached.
     */
    public void loginFailed(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        String attemptsKey = ATTEMPTS_PREFIX + phoneNumber;
        String lockoutKey = LOCKOUT_PREFIX + phoneNumber;

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);

        if (attempts != null && attempts == 1) {
            // Expire attempt count key after 10 minutes of inactivity
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(10));
        }

        log.warn("Failed login attempt #{} for phone number: {}", attempts, phoneNumber);

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(lockoutKey, "LOCKED", Duration.ofMinutes(LOCK_TIME_MINUTES));
            redisTemplate.delete(attemptsKey); // Clear attempts counter once locked out
            log.warn("Account locked for phone number: {} for {} minutes", phoneNumber, LOCK_TIME_MINUTES);
        }
    }

    /**
     * Clears failed attempt counters when user logs in successfully.
     */
    public void loginSucceeded(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }
        redisTemplate.delete(ATTEMPTS_PREFIX + phoneNumber);
        redisTemplate.delete(LOCKOUT_PREFIX + phoneNumber);
    }

    /**
     * Returns remaining locked time in seconds (for response message formatting).
     */
    public long getRemainingLockoutTimeSeconds(String phoneNumber) {
        String lockoutKey = LOCKOUT_PREFIX + phoneNumber;
        Long expireTime = redisTemplate.getExpire(lockoutKey);
        return (expireTime != null && expireTime > 0) ? expireTime : 0;
    }
}