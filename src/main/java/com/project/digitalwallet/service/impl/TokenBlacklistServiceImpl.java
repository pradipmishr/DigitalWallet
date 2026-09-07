package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String jti, Duration ttl) {

        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }

        redisTemplate.opsForValue()
                .set(PREFIX + jti, "1", ttl);
    }

    public boolean isBlacklisted(String jti) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX + jti)
        );
    }
}
