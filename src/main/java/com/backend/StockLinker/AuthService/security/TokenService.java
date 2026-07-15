package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.enums.AuditAction;
import com.backend.StockLinker.AuthService.enums.ResourceType;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.RefreshToken;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.service.AuditService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Value("${security.cookie.secure:false}")
    private boolean secure;

    @Value("${security.cookie.domain:localhost}")
    private String domain;

    // ==========================================
    // 1. GENERATE BOTH TOKENS (LOGIN FLOW)
    // ==========================================
    public TokenPair generate(User user, String deviceId, HttpServletResponse response) {

        String accessToken = jwtService.generateAccessToken(user, deviceId);
        RefreshToken refreshToken = refreshTokenService.create(user, deviceId);

        setAccessCookie(response, accessToken);
        setRefreshCookie(response, refreshToken.getToken());

        auditService.log(AuditLogRequest.builder()
                .userId(user.getId())
                .action(AuditAction.ACCESS_TOKEN_CREATED)
                .resourceType(ResourceType.TOKEN)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .build());

        return new TokenPair(accessToken, refreshToken.getToken());
    }

    // ==========================================
    // 2. GENERATE ONLY ACCESS TOKEN (REFRESH FLOW)
    // ==========================================
    public String generateAccessTokenOnly(User user, String deviceId, HttpServletResponse response) {

        String accessToken = jwtService.generateAccessToken(user, deviceId);

        setAccessCookie(response, accessToken);

        auditService.log(AuditLogRequest.builder()
                .userId(user.getId())
                .action(AuditAction.ACCESS_TOKEN_CREATED)
                .resourceType(ResourceType.TOKEN)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .build());

        return accessToken;
    }

    public void clear(HttpServletResponse response) {
        deleteCookie(response, "accessToken", "/");
        deleteCookie(response, "refreshToken", "/api/auth");
    }

    private void setAccessCookie(HttpServletResponse res, String token) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .domain(domain.equals("localhost") ? null : domain)
                .sameSite("Lax")
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void setRefreshCookie(HttpServletResponse res, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(secure)
                // Widen path to /api/auth so /session, /refresh, and /logout can read it
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .domain(domain.equals("localhost") ? null : domain)
                .sameSite("Lax")
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteCookie(HttpServletResponse res, String name, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(0)
                .domain(domain.equals("localhost") ? null : domain)
                .sameSite("Lax")
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}