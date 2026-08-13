package com.project.digitalwallet.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.digitalwallet.annotation.Idempotent;
import com.project.digitalwallet.dto.IdempotencyRecord;
import com.project.digitalwallet.security.UserPrincipal;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object enforceIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        String idempotencyKey = request.getHeader(idempotent.headerName());

        // 1. Skip if no idempotency header is present
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        String redisKey = "idempotency:" + idempotencyKey;
        String currentPayloadHash = hashRequestBody(joinPoint.getArgs());

        // 2. Fetch and parse existing record from Redis safely
        Object rawData = redisTemplate.opsForValue().get(redisKey);
        IdempotencyRecord record = parseRecord(rawData);

        if (record != null) {
            // Case A: Request currently being processed by another thread/instance
            if (record.getStatus() == IdempotencyRecord.Status.IN_PROGRESS) {
                return buildResponse(joinPoint, HttpStatus.CONFLICT, "A request with this Idempotency-Key is currently in progress.");
            }

            // Case B: Completed execution — handle replay
            if (record.getStatus() == IdempotencyRecord.Status.COMPLETED) {
                // Verify payload match
                if (!record.getRequestHash().equals(currentPayloadHash)) {
                    return buildResponse(joinPoint, HttpStatus.BAD_REQUEST, "Payload mismatch for the provided Idempotency-Key.");
                }

                // Return cached response matching original endpoint signature
                return buildResponse(joinPoint, HttpStatus.valueOf(record.getHttpStatusCode()), record.getResponseBody());
            }
        }

        // 3. Lock key: Mark IN_PROGRESS (2-minute lock window)
        IdempotencyRecord progressRecord = IdempotencyRecord.builder()
                .status(IdempotencyRecord.Status.IN_PROGRESS)
                .requestHash(currentPayloadHash)
                .build();

        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, progressRecord, Duration.ofMinutes(2));

        if (Boolean.FALSE.equals(lockAcquired)) {
            return buildResponse(joinPoint, HttpStatus.CONFLICT, "A request with this Idempotency-Key is currently in progress.");
        }

        try {
            // 4. Proceed with actual controller/service execution
            Object result = joinPoint.proceed();

            int statusCode = HttpStatus.OK.value();
            Object responseBody = result;

            if (result instanceof ResponseEntity<?> responseEntity) {
                statusCode = responseEntity.getStatusCode().value();
                responseBody = responseEntity.getBody();
            }

            // 5. Store final response with configured TTL
            IdempotencyRecord completedRecord = IdempotencyRecord.builder()
                    .status(IdempotencyRecord.Status.COMPLETED)
                    .requestHash(currentPayloadHash)
                    .httpStatusCode(statusCode)
                    .responseBody(responseBody)
                    .build();

            redisTemplate.opsForValue().set(
                    redisKey,
                    completedRecord,
                    Duration.ofSeconds(idempotent.expireInSeconds())
            );

            return result;

        } catch (Throwable ex) {
            // Evict lock key on exception so client can retry safely
            redisTemplate.delete(redisKey);
            throw ex;
        }
    }

    /** Calculates SHA-256 hash of DTO inputs while ignoring infrastructure/framework parameters. */
    private String hashRequestBody(Object[] args) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object arg : args) {
                if (arg != null && isPayloadArgument(arg)) {
                    byte[] jsonBytes = objectMapper.writeValueAsBytes(arg);
                    digest.update(jsonBytes);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ex) {
            log.error("Failed to compute idempotency payload hash", ex);
            return "";
        }
    }

    private boolean isPayloadArgument(Object arg) {
        return !(arg instanceof ServletRequest)
                && !(arg instanceof ServletResponse)
                && !(arg instanceof UserPrincipal)
                && !(arg instanceof Authentication)
                && !(arg instanceof BindingResult);
    }

    /** Converts Redis data safely into IdempotencyRecord across different Jackson serializers. */
    private IdempotencyRecord parseRecord(Object rawData) {
        if (rawData == null) return null;
        if (rawData instanceof IdempotencyRecord record) return record;
        try {
            return objectMapper.convertValue(rawData, IdempotencyRecord.class);
        } catch (Exception e) {
            log.warn("Could not deserialize Redis value to IdempotencyRecord", e);
            return null;
        }
    }

    /** Formats returned responses based on whether controller uses ResponseEntity or direct DTO/Wrapper. */
    private Object buildResponse(ProceedingJoinPoint joinPoint, HttpStatus status, Object body) {
        boolean returnsResponseEntity = joinPoint.getSignature().toString().contains("ResponseEntity");
        if (returnsResponseEntity) {
            return ResponseEntity.status(status).body(body);
        }
        return body;
    }
}