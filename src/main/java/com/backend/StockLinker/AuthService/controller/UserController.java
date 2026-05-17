package com.backend.StockLinker.AuthService.controller;

import com.backend.StockLinker.AuthService.exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.AuthService.exception.customExceptions.UnauthorizedException;
import com.backend.StockLinker.AuthService.dto.request.RoleSelectRequest;
import com.backend.StockLinker.AuthService.dto.response.DeviceInfoResponse;
import com.backend.StockLinker.AuthService.dto.response.UserInfoResponse;
import com.backend.StockLinker.AuthService.mapper.DeviceMapper;
import com.backend.StockLinker.AuthService.mapper.UserMapper;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.model.UserDevice;
import com.backend.StockLinker.AuthService.repository.UserDeviceRepository;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.service.RoleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final DeviceMapper deviceMapper;

    // =========================================================
    // 🎯 GET CURRENT USER INFO (USING MAPPER)
    // =========================================================
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String userId = auth.getName();
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isNewUser = user.getRoles().isEmpty();

        // Using mapper instead of direct factory method
        UserInfoResponse response = userMapper.toUserInfoResponse(user, isNewUser);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 🎯 GET USER DEVICES (USING MAPPER)
    // =========================================================
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceInfoResponse>> getUserDevices(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String userId = auth.getName();
        List<UserDevice> devices = userDeviceRepository.findByUserId(userId);

        // Using mapper for conversion
        List<DeviceInfoResponse> response = deviceMapper.toDeviceInfoResponseList(devices);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 🎯 SELECT/UPGRADE ROLE
    // =========================================================
    @PostMapping("/role/select")
    public ResponseEntity<Map<String, String>> selectRole(
            @Valid @RequestBody RoleSelectRequest request,
            Authentication auth,
            HttpServletRequest httpRequest
    ) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String userId = auth.getName();
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        roleService.upgradeRole(user, request.getRole(), httpRequest);

        return ResponseEntity.ok(Map.of(
                "message", "Role upgraded to " + request.getRole(),
                "role", request.getRole()
        ));
    }

    // =========================================================
    // 🎯 CHECK IF USER HAS BUSINESS ROLE
    // =========================================================
    @GetMapping("/role/business")
    public ResponseEntity<Map<String, Object>> hasBusinessRole(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String userId = auth.getName();
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasBusinessRole = roleService.hasBusinessRole(user);
        String businessRole = roleService.getBusinessRole(user);

        return ResponseEntity.ok(Map.of(
                "hasBusinessRole", hasBusinessRole,
                "businessRole", businessRole != null ? businessRole : "NONE"
        ));
    }
}