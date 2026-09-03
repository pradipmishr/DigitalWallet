package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.KycStatus;
import com.project.digitalwallet.common.enums.WalletStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UserByIdDto {
    private Long id;
    private String fullName;
    private KycStatus kycStatus;
    private WalletStatus walletStatus;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private LocalDateTime createdAt;
    private Map<String, List<TransactionDto>> transactions;
    private AddressDto address;
}
