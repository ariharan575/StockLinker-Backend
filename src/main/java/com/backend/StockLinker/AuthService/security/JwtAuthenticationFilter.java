package com.backend.StockLinker.AuthService.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token != null && jwtService.validateToken(token)) {

                String tokenType = jwtService.extractTokenType(token);

                // ✅ ONLY ACCESS TOKEN
                if ("access".equals(tokenType)) {

                    String userId = jwtService.extractUserId(token);
                    Set<String> roles = jwtService.extractRoles(token);

                    List<SimpleGrantedAuthority> authorities =
                            roles.stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                    .collect(Collectors.toList());

                    // fallback (guest/user safety)
                    if (authorities.isEmpty()) {
                        authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                    }

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(auth);

                    log.debug("Authenticated user: {} roles: {}", userId, roles);
                }
            }

        } catch (ExpiredJwtException e) {
            log.warn("Token expired");
        } catch (Exception e) {
            log.error("JWT error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {

        // 1️⃣ Authorization header
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // 2️⃣ Cookie fallback (IMPORTANT for your system)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Exclude only login, refresh, OAuth endpoints from filtering
        if (path.equals("/api/auth/phone/login") ||
                path.equals("/api/auth/guest/login") ||
                path.equals("/api/auth/refresh") ||
                path.startsWith("/oauth2/")) {
            return true;
        }

        // All other /api/auth/* endpoints will be filtered (including /me)
        return false;
    }
}