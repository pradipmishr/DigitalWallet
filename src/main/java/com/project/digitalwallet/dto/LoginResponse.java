package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.UserRole;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String token;

    private String tokenType;

    private Long userId;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private UserRole role;
}