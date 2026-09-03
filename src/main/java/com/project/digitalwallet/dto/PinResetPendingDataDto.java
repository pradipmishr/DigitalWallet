package com.project.digitalwallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinResetPendingDataDto {

    private Long userId;

    private String email;

    private int attempts;
}
