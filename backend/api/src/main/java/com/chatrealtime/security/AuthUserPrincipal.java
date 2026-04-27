package com.chatrealtime.security;

import com.chatrealtime.domain.User;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Security principal. Password hash is omitted from Jackson serialization so Redis
 * JSON caches never store bcrypt hashes.
 */
public class AuthUserPrincipal implements UserDetails {
    @Getter
    private final String id;
    @Getter
    private final String username;
    /** Hash from DB; never exposed as a JSON property (Redis cache). */
    @JsonIgnore
    private final String password;
    @Getter
    private final int tokenVersion;

    @JsonCreator
    public AuthUserPrincipal(
            @JsonProperty("id") String id,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("tokenVersion") int tokenVersion
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.tokenVersion = tokenVersion;
    }

    /**
     * Full principal from persistence (includes password hash for {@link UserDetails}).
     */
    public static AuthUserPrincipal from(User user) {
        return new AuthUserPrincipal(user.getId(), user.getUsername(), user.getPassword(), user.getTokenVersion());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
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
