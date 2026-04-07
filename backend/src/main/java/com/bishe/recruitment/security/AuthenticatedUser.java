package com.bishe.recruitment.security;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String displayName;
    private final List<String> roles;
    private final List<SimpleGrantedAuthority> authorities;

    public AuthenticatedUser(Long userId, String username, String password, String displayName, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.roles = roles;
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
