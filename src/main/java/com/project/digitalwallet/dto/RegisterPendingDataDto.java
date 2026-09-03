package com.project.digitalwallet.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPendingDataDto {
    private String firstName;

    private String lastName;

    private String email;

    private LocalDate dateOfBirth;

    private String phoneNumber;

    private String password;
    private int attempts;
}
