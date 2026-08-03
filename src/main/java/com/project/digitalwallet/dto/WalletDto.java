package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WalletDto {
     private Long id;

    private BigDecimal balance;

    private WalletStatus status;

}
