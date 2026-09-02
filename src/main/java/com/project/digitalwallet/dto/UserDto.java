package com.project.digitalwallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserDto {
    private Long id;
    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;
    private LocalDate dateOfBirth;

    private WalletDto wallet;
}
