package com.project.digitalwallet.security;

import com.project.digitalwallet.common.enums.UserRole;
import com.project.digitalwallet.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private final User user;
    private final String username;
    private final String password;
    private final UserRole role;

    public UserPrincipal(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        this.user = user;
        this.username = user.getPhoneNumber();
        this.password = user.getPassword();
        this.role = user.getRole();
    }

    // Used when creating UserPrincipal from JWT claims
    public UserPrincipal(String username, String password, UserRole role) {
        this.user = null;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User getUser() {
        return user;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role != null
                ? Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role.name()))
                : Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}