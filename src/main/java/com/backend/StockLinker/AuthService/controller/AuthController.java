package com.backend.StockLinker.AuthService.controller;

import com.backend.StockLinker.AuthService.dto.request.PhoneOtpRequest;
import com.backend.StockLinker.AuthService.dto.request.RoleSelectRequest;
import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.exception.ValidationException;
import com.backend.StockLinker.AuthService.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/phone/login")
    public ResponseEntity<AuthResponse> phoneLogin(
            @Valid @RequestBody PhoneOtpRequest requestDto,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            throw new ValidationException(errors);
        }

        String deviceId = (String) request.getAttribute("deviceId");
        log.debug("Phone login attempt for device: {}", deviceId);

        return ResponseEntity.ok(authService.phoneLogin(requestDto.getIdToken(), deviceId, request, response));
    }

    @PostMapping("/guest/login")
    public ResponseEntity<AuthResponse> guestLogin(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String deviceId = (String) request.getAttribute("deviceId");

        if (deviceId == null || deviceId.isBlank()) {
            log.warn("Guest login attempt without device ID");
            throw new BaseException(ErrorCode.BAD_REQUEST, "Device ID is required for guest login");
        }

        log.debug("Guest login attempt for device: {}", deviceId);
        return ResponseEntity.ok(authService.guestLogin(deviceId, request, response));
    }

    @PostMapping("/role/select")
    public ResponseEntity<AuthResponse> selectRole(
            @Valid @RequestBody RoleSelectRequest requestDto,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            throw new ValidationException(errors);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Role selection attempted without authentication");
            throw new BaseException(ErrorCode.UNAUTHORIZED, "You must be authenticated to select a role");
        }

        String userId = authentication.getName();
        String deviceId = (String) request.getAttribute("deviceId");

        log.debug("Role selection for user {}: {}", userId, requestDto.getRole());

        return ResponseEntity.ok(authService.selectRole(userId, requestDto.getRole(), deviceId, request, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Refresh attempt without refresh token");
            throw new BaseException(ErrorCode.UNAUTHORIZED, "Refresh token is missing");
        }

        String deviceId = (String) request.getAttribute("deviceId");
        log.debug("Token refresh attempt for device: {}", deviceId);

        return ResponseEntity.ok(authService.refresh(refreshToken, deviceId, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Logout attempt without refresh token");
            throw new BaseException(ErrorCode.UNAUTHORIZED, "Refresh token is required for logout");
        }

        String deviceId = (String) request.getAttribute("deviceId");
        authService.logout(refreshToken, deviceId, request, response);

        log.debug("Logout successful for device: {}", deviceId);
        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully",
                "status", "success"
        ));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Global logout attempted without authentication");
            throw new BaseException(ErrorCode.UNAUTHORIZED, "You must be authenticated for global logout");
        }

        String userId = authentication.getName();
        String deviceId = (String) request.getAttribute("deviceId");

        authService.logoutAll(userId, deviceId, request, response);

        log.debug("Global logout successful for user: {}", userId);
        return ResponseEntity.ok(Map.of(
                "message", "Global logout successful",
                "status", "success"
        ));
    }
}