package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.LoginRequest;
import com.project.digitalwallet.dto.LoginResponse;

public interface AuthService {
    public LoginResponse login(LoginRequest request);
}
