package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.constant.AuditAction;
import com.backend.StockLinker.AuthService.exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.model.Permission;
import com.backend.StockLinker.AuthService.model.Role;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.PermissionRepository;
import com.backend.StockLinker.AuthService.repository.RoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    // =========================================================
    // 🔍 GET PERMISSION BY NAME
    // =========================================================
    public Permission getPermissionByName(String name) {
        return permissionRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + name));
    }

    // =========================================================
    // ✅ CHECK IF PERMISSION EXISTS
    // =========================================================
    public boolean permissionExists(String name) {
        return permissionRepository.existsByName(name);
    }

    // =========================================================
    // 📋 GET ALL PERMISSIONS
    // =========================================================
    public Set<Permission> getAllPermissions() {
        return Set.copyOf(permissionRepository.findAll());
    }

    // =========================================================
    // 📋 GET PERMISSIONS BY RESOURCE
    // =========================================================
    public Set<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource);
    }

    // =========================================================
    // ✅ CHECK USER PERMISSION
    // =========================================================
    public boolean hasPermission(User user, String permissionName) {
        if (user == null) return false;
        return user.hasPermission(permissionName);
    }

    // =========================================================
    // ✅ CHECK USER PERMISSION (BY USER ID)
    // =========================================================
    @Transactional(readOnly = true)
    public boolean hasPermission(String userId, String permissionName) {
        // This would need UserRepository - but keeping for reference
        return false;
    }

    // =========================================================
    // 🔐 CHECK AND THROW IF NO PERMISSION
    // =========================================================
    public void checkPermission(User user, String permissionName,
                                HttpServletRequest request, String action) {
        if (!hasPermission(user, permissionName)) {
            auditService.log(auditService.failure(
                    user,
                    action,
                    "Missing permission: " + permissionName,
                    getClientIp(request),
                    request.getHeader("User-Agent")
            ));
            throw new BaseException(ErrorCode.INSUFFICIENT_PERMISSIONS);
        }
    }

    // =========================================================
    // ➕ ASSIGN PERMISSION TO ROLE
    // =========================================================
    public void assignPermissionToRole(String roleName, String permissionName,
                                       User adminUser, HttpServletRequest request) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionName));

        boolean added = role.getPermissions().add(permission);

        if (added) {
            roleRepository.save(role);

            auditService.log(auditService.successWithChanges(
                    adminUser,
                    AuditAction.PERMISSION_GRANTED.name(),
                    "ROLE",
                    role.getId(),
                    null,
                    permissionName,
                    getClientIp(request),
                    request.getHeader("User-Agent")
            ));

            log.info("Permission {} granted to role {}", permissionName, roleName);
        }
    }

    // =========================================================
    // ➖ REMOVE PERMISSION FROM ROLE
    // =========================================================
    public void removePermissionFromRole(String roleName, String permissionName,
                                         User adminUser, HttpServletRequest request) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionName));

        boolean removed = role.getPermissions().remove(permission);

        if (removed) {
            roleRepository.save(role);

            auditService.log(auditService.successWithChanges(
                    adminUser,
                    AuditAction.PERMISSION_REVOKED.name(),
                    "ROLE",
                    role.getId(),
                    permissionName,
                    null,
                    getClientIp(request),
                    request.getHeader("User-Agent")
            ));

            log.info("Permission {} revoked from role {}", permissionName, roleName);
        }
    }

    // =========================================================
    // 📋 GET PERMISSIONS FOR ROLE
    // =========================================================
    public Set<String> getRolePermissions(String roleName) {
        Role role = roleRepository.findByNameWithPermissions(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        return role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    // =========================================================
    // 🌐 IP HELPER
    // =========================================================
    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0] : request.getRemoteAddr();
    }
}