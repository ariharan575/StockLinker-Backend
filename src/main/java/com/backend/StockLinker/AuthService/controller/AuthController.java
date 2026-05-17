package com.backend.StockLinker.AuthService.controller;

import com.backend.StockLinker.AuthService.dto.request.PhoneOtpRequest;
import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.service.AuthFlowService;
import com.backend.StockLinker.AuthService.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AuthFlowService authFlowService;
    private final UserRepository userRepository;

    // =========================================================
    // 📱 PHONE OTP LOGIN
    // =========================================================
    @PostMapping("/phone/login")
    public ResponseEntity<AuthResponse> phoneLogin(
            @Valid @RequestBody PhoneOtpRequest request,
            @CookieValue(value = "deviceId", required = false) String deviceId,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        log.info("Phone login attempt with deviceId: {}", deviceId);
        AuthResponse authResponse = authService.loginWithPhoneOtp(
                request.getIdToken(),
                deviceId,
                httpRequest,
                response
        );
        return ResponseEntity.ok(authResponse);
    }

    // =========================================================
    // 👤 GUEST LOGIN
    // =========================================================
    @PostMapping("/guest/login")
    public ResponseEntity<AuthResponse> guestLogin(
            @CookieValue(value = "deviceId", required = false) String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.info("Guest login attempt with deviceId: {}", deviceId);
        AuthResponse authResponse = authService.guestLogin(deviceId, request, response);
        return ResponseEntity.ok(authResponse);
    }

    // =========================================================
    // 🔄 REFRESH TOKEN
    // =========================================================
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @CookieValue(value = "deviceId", required = false) String deviceId,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BaseException(ErrorCode.INVALID_TOKEN, "Refresh token is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Device ID is required");
        }

        log.info("Token refresh attempt for device: {}", deviceId);
        AuthResponse authResponse = authFlowService.refresh(refreshToken, deviceId, response);
        return ResponseEntity.ok(authResponse);
    }

    // =========================================================
    // 🚪 LOGOUT (SINGLE DEVICE)
    // =========================================================
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @CookieValue(value = "deviceId", required = false) String deviceId,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BaseException(ErrorCode.INVALID_TOKEN, "Refresh token is required");
        }

        authFlowService.logout(refreshToken, deviceId, response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // =========================================================
    // 🚪 LOGOUT ALL DEVICES
    // =========================================================
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            Authentication auth,
            HttpServletRequest request
    ) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        String userId = auth.getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        authFlowService.logoutAll(user, request);
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices successfully"));
    }

    // =========================================================
    // 🌐 OAUTH SUCCESS (FRONTEND REDIRECT)
    // =========================================================
    @GetMapping("/oauth-success")
    public ResponseEntity<Map<String, String>> oauthSuccess(@RequestParam String token) {
        return ResponseEntity.ok(Map.of("accessToken", token));
    }

    // =========================================================
    // ✅ HEALTH CHECK
    // =========================================================
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "AuthService"));
    }
}