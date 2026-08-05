package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.dto.LoginRequest;
import com.project.digitalwallet.dto.LoginResponse;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.security.JwtUtil;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getPhoneNumber(),
                                request.getPassword()
                        )
                );

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        User user = principal.getUser();

        String token = jwtUtil.generateToken(principal);

        return UserMapper.toLoginResponse(user, token);
    }
}