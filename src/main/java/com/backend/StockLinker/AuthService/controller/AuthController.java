package com.backend.StockLinker.AuthService.controller;

import com.backend.StockLinker.AuthService.dto.request.PhoneOtpRequest;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.service.AuthFlowService;
import com.backend.StockLinker.AuthService.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthFlowService authFlowService;
    private final UserRepository userRepository;

    // =========================================================
    // 📱 PHONE OTP LOGIN
    // =========================================================
    @PostMapping("/phone/login")
    public Map<String, Object> phoneLogin(
            @RequestBody PhoneOtpRequest request,
            @CookieValue(value = "deviceId", required = false) String deviceId,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        return authService.loginWithPhoneOtp(
                request.getIdToken(),
                deviceId,
                httpRequest,
                response
        );
    }

    // =========================================================
    // 👤 GUEST LOGIN
    // =========================================================
    @PostMapping("/guest/login")
    public Map<String, Object> guestLogin(
            @CookieValue(value = "deviceId", required = false) String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return authService.guestLogin(deviceId, request, response);
    }

    // =========================================================
    // 🔄 REFRESH TOKEN (DEVICE SAFE)
    // =========================================================
    @PostMapping("/refresh")
    public Map<String, Object> refresh(
            @CookieValue("refreshToken") String refreshToken,
            @CookieValue("deviceId") String deviceId,
            HttpServletResponse response
    ) {
        return authFlowService.refresh(refreshToken, deviceId, response);
    }

    // =========================================================
    // 🚪 LOGOUT (SINGLE DEVICE)
    // =========================================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue("refreshToken") String refreshToken,
            @CookieValue("deviceId") String deviceId,
            HttpServletResponse response
    ) {

        authFlowService.logout(refreshToken, deviceId, response);

        return ResponseEntity.ok("Logged out successfully");
    }

    // =========================================================
    // 🚪 LOGOUT ALL DEVICES
    // =========================================================
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(Authentication auth) {

        String userId = auth.getName();

        User user = userRepository.findById(userId)
                .orElseThrow();

        authFlowService.logoutAll(user);

        return ResponseEntity.ok("Logged out from all devices");
    }

    // =========================================================
    // 🌐 OAUTH SUCCESS (FRONTEND REDIRECT)
    // =========================================================
    @GetMapping("/oauth-success")
    public Map<String, String> oauthSuccess(@RequestParam String token) {
        return Map.of("accessToken", token);
    }
}