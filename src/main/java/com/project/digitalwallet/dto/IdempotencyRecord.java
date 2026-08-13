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
    private String requestHash;    // SHA-256 hash of the request body to detect payload mismatches
    private int httpStatusCode;    // e.g. 200, 201
    private Object responseBody;   // Response Wrapper/DTO returned to client
}