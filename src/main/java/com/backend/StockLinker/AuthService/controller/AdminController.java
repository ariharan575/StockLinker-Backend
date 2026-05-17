package com.backend.StockLinker.AuthService.controller;

import com.backend.StockLinker.AuthService.exception.customExceptions.ForbiddenException;
import com.backend.StockLinker.AuthService.exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.AuthService.exception.customExceptions.UnauthorizedException;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.service.PermissionService;
import com.backend.StockLinker.AuthService.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PermissionService permissionService;

    // =========================================================
    // 👑 CHECK ADMIN ACCESS (USING CUSTOM EXCEPTION)
    // =========================================================
    private void checkAdminAccess(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        User user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!roleService.hasRole(user, "ADMIN")) {
            throw new ForbiddenException("Admin access required");
        }

        // Check admin permission using permission service
        permissionService.checkPermission(user, "ADMIN_ACCESS", null, "ADMIN_ACCESS");
    }

    // =========================================================
    // 🎯 ASSIGN ROLE TO USER (ADMIN ONLY)
    // =========================================================
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Map<String, String>> assignRoleToUser(
            @PathVariable String userId,
            @RequestParam String roleName,
            Authentication auth,
            HttpServletRequest request
    ) {
        checkAdminAccess(auth);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        roleService.upgradeRole(user, roleName, request);

        return ResponseEntity.ok(Map.of(
                "message", "Role " + roleName + " assigned to user " + userId,
                "userId", userId,
                "role", roleName
        ));
    }

    // =========================================================
    // 🗑️ REMOVE ROLE FROM USER (ADMIN ONLY)
    // =========================================================
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<Map<String, String>> removeRoleFromUser(
            @PathVariable String userId,
            @PathVariable String roleName,
            Authentication auth,
            HttpServletRequest request
    ) {
        checkAdminAccess(auth);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        roleService.removeRole(user, roleName, request);

        return ResponseEntity.ok(Map.of(
                "message", "Role " + roleName + " removed from user " + userId,
                "userId", userId,
                "role", roleName
        ));
    }

    // =========================================================
    // 🔧 ASSIGN PERMISSION TO ROLE (ADMIN ONLY)
    // =========================================================
    @PostMapping("/roles/{roleName}/permissions")
    public ResponseEntity<Map<String, String>> assignPermissionToRole(
            @PathVariable String roleName,
            @RequestParam String permissionName,
            Authentication auth,
            HttpServletRequest request
    ) {
        checkAdminAccess(auth);

        User admin = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        permissionService.assignPermissionToRole(roleName, permissionName, admin, request);

        return ResponseEntity.ok(Map.of(
                "message", "Permission " + permissionName + " assigned to role " + roleName,
                "role", roleName,
                "permission", permissionName
        ));
    }

    // =========================================================
    // 🔧 REMOVE PERMISSION FROM ROLE (ADMIN ONLY)
    // =========================================================
    @DeleteMapping("/roles/{roleName}/permissions/{permissionName}")
    public ResponseEntity<Map<String, String>> removePermissionFromRole(
            @PathVariable String roleName,
            @PathVariable String permissionName,
            Authentication auth,
            HttpServletRequest request
    ) {
        checkAdminAccess(auth);

        User admin = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        permissionService.removePermissionFromRole(roleName, permissionName, admin, request);

        return ResponseEntity.ok(Map.of(
                "message", "Permission " + permissionName + " removed from role " + roleName,
                "role", roleName,
                "permission", permissionName
        ));
    }

    // =========================================================
    // 📋 GET USER PERMISSIONS (ADMIN ONLY)
    // =========================================================
    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(@PathVariable String userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getName())
                .collect(java.util.stream.Collectors.toSet());

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "roles", roleService.getUserRoles(user),
                "permissions", permissions
        ));
    }
}