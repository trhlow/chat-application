package com.chatrealtime.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final UserPrincipalService userPrincipalService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        Optional<Claims> claims = jwtTokenService.parseValidClaims(token);
        if (claims.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = claims.get().getSubject();
        int tokenVersion = extractTokenVersion(claims.get());
        AuthUserPrincipal principal = loadPrincipal(userId);
        if (principal == null
                || principal.getTokenVersion() != tokenVersion
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }

    private AuthUserPrincipal loadPrincipal(String userId) {
        try {
            return userPrincipalService.loadByUserId(userId);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static int extractTokenVersion(Claims claims) {
        Object tokenVersion = claims.get("tokenVersion");
        if (tokenVersion instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}

