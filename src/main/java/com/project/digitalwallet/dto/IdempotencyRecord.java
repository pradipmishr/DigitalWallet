package com.project.digitalwallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord implements Serializable {

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }

    private Status status;
    private String requestHash;    // SHA-256 hash of request body

    @Builder.Default
    private Integer httpStatusCode = 0; // Use Integer wrapper instead of primitive int

    private Object responseBody;   // Response DTO/Wrapper
}