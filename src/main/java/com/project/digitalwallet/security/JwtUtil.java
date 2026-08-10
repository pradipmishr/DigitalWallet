package com.project.digitalwallet.security;

import com.project.digitalwallet.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

//    public String generateToken(UserPrincipal userPrincipal) {
//        return Jwts.builder()
//                .subject(userPrincipal.getUsername())
//                .claim("role", userPrincipal.getRole().name())
//                .issuedAt(new Date())
//                .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000 )) // 1 hour
//                .signWith(key)
//                .compact();
//    }
    public String generateToken(UserPrincipal userPrincipal) {
        long now = System.currentTimeMillis();
        long expirationTimeMs = 3600000L; // 1 hour explicitly as long (or 86400000L for 24 hours)

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("role", userPrincipal.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationTimeMs))
                .signWith(key)
                .compact();
    }

//    private Claims extractAllClaims(String token) {
//        return Jwts.parser()
//                .verifyWith(key)
//                .build()
//                .parseSignedClaims(token)
//                .getPayload();
//    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(604800) // 7 days tolerance for clock skew
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UserRole extractRole(String token) {
        return UserRole.valueOf(extractAllClaims(token).get("role", String.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()); // Ignore expiration check
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }
}