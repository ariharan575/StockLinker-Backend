package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.model.RefreshToken;
import com.backend.StockLinker.AuthService.model.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${security.cookie.secure:false}")
    private boolean secure;

    @Value("${security.cookie.domain:localhost}")
    private String domain;

    // =========================================================
    // ✅ GENERATE TOKENS (LOGIN ONLY)
    // =========================================================
    public TokenPair generate(User user, String deviceId, HttpServletResponse response) {

        // 🔐 ACCESS TOKEN
        String accessToken = jwtService.generateAccessToken(user);

        // 🔐 REFRESH TOKEN (DEVICE LINKED)
        RefreshToken refreshToken =
                refreshTokenService.create(user, deviceId);

        // 🍪 COOKIES
        setAccessCookie(response, accessToken);
        setRefreshCookie(response, refreshToken.getToken());
        setDeviceCookie(response, deviceId);

        return new TokenPair(accessToken, refreshToken.getToken());
    }

    // =========================================================
    // 🔄 REFRESH TOKEN FLOW (FIXED)
    // =========================================================
    public TokenPair refresh(
            RefreshToken rotatedToken,
            String deviceId,
            HttpServletResponse response
    ) {

        User user = rotatedToken.getUser();

        // 🔐 NEW ACCESS TOKEN ONLY
        String newAccess = jwtService.generateAccessToken(user);

        // ❗ DO NOT CREATE NEW REFRESH AGAIN (ALREADY ROTATED)
        String newRefresh = rotatedToken.getToken();

        setAccessCookie(response, newAccess);
        setRefreshCookie(response, newRefresh);
        setDeviceCookie(response, deviceId);

        return new TokenPair(newAccess, newRefresh);
    }

    // =========================================================
    // 🚪 CLEAR TOKENS (LOGOUT)
    // =========================================================
    public void clear(HttpServletResponse response) {

        deleteCookie(response, "accessToken", "/");
        deleteCookie(response, "refreshToken", "/api/auth/refresh");
        deleteCookie(response, "deviceId", "/"); // 🔥 IMPORTANT
    }

    // =========================================================
    // 🍪 COOKIE METHODS
    // =========================================================

    private void setAccessCookie(HttpServletResponse res, String token) {

        Cookie cookie = new Cookie("accessToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60); // 15 min
        cookie.setDomain(domain);

        res.addCookie(cookie);
    }

    private void setRefreshCookie(HttpServletResponse res, String token) {

        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setDomain(domain);

        res.addCookie(cookie);
    }

    private void setDeviceCookie(HttpServletResponse res, String deviceId) {

        Cookie cookie = new Cookie("deviceId", deviceId);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        cookie.setDomain(domain);

        res.addCookie(cookie);
    }

    private void deleteCookie(HttpServletResponse res, String name, String path) {

        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(path);
        cookie.setMaxAge(0);
        cookie.setDomain(domain);

        res.addCookie(cookie);
    }

    // =========================================================
    // DTO
    // =========================================================
    public record TokenPair(String accessToken, String refreshToken) {}
}