package com.project.digitalwallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserDto {
    private Long id;
    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;
    private Date dateOfBirth;

    private WalletDto wallet;
}
