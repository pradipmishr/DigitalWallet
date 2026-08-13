package com.project.digitalwallet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisConnectionTest implements CommandLineRunner {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(String... args) {
        try {
            redisTemplate.opsForValue().set("ping", "pong");
            Object value = redisTemplate.opsForValue().get("ping");
            log.info("Redis Connection Successful! Ping-Pong test response: {}", value);
        } catch (Exception e) {
            log.error("Failed to connect to Redis: {}", e.getMessage());
        }
    }
}