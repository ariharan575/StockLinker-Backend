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
import com.backend.StockLinker.AuthService.repository.UserRepository;
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
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // =========================================================
    // 🔍 CHECK IF USER HAS BUSINESS ROLE (SHOPKEEPER OR WHOLESALER)
    // =========================================================
    public boolean hasBusinessRole(User user) {
        return user.getRoles()
                .stream()
                .anyMatch(r -> r.getName().equals("SHOPKEEPER") || r.getName().equals("WHOLESALER"));
    }

    // =========================================================
    // 🔍 GET USER'S BUSINESS ROLE
    // =========================================================
    public String getBusinessRole(User user) {
        return user.getRoles()
                .stream()
                .filter(r -> r.getName().equals("SHOPKEEPER") || r.getName().equals("WHOLESALER"))
                .map(Role::getName)
                .findFirst()
                .orElse(null);
    }

    // =========================================================
    // 📋 GET ALL USER ROLES AS STRING SET
    // =========================================================
    public Set<String> getUserRoles(User user) {
        return user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    // =========================================================
    // 👤 ASSIGN GUEST ROLE TO NEW USER
    // =========================================================
    public void assignGuestRole(User user) {
        Role guestRole = roleRepository.findByName("GUEST")
                .orElseThrow(() -> new RuntimeException("Role GUEST not found - please initialize roles first"));

        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean alreadyExists = dbUser.getRoles()
                .stream()
                .anyMatch(r -> r.getId().equals(guestRole.getId()));

        if (!alreadyExists) {
            dbUser.getRoles().add(guestRole);
            userRepository.save(dbUser);
            log.info("GUEST role assigned to user: {}", dbUser.getId());
        }
    }

    // =========================================================
    // ⬆️ UPGRADE USER ROLE (GUEST → SHOPKEEPER OR WHOLESALER)
    // =========================================================
    public void upgradeRole(User user, String roleName, HttpServletRequest request) {

        // Validate role name
        if (!roleName.equals("SHOPKEEPER") && !roleName.equals("WHOLESALER")) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Invalid role. Allowed: SHOPKEEPER, WHOLESALER");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BaseException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + roleName));

        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user already has a business role
        if (hasBusinessRole(dbUser)) {
            throw new BaseException(ErrorCode.ROLE_ALREADY_ASSIGNED, "User already has a business role");
        }

        // Prevent duplicate role
        boolean alreadyExists = dbUser.getRoles()
                .stream()
                .anyMatch(r -> r.getName().equals(roleName));

        if (!alreadyExists) {
            dbUser.getRoles().add(role);
            userRepository.save(dbUser);

            // Audit log for role upgrade
            auditService.log(auditService.success(
                    dbUser,
                    AuditAction.ROLE_ASSIGNED.name(),
                    "ROLE",
                    role.getId(),
                    getClientIp(request),
                    request.getHeader("User-Agent")
            ));

            log.info("User {} upgraded to role {}", user.getId(), roleName);
        }
    }

    // =========================================================
    // 🗑️ REMOVE ROLE FROM USER
    // =========================================================
    public void removeRole(User user, String roleName, HttpServletRequest request) {

        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        boolean removed = dbUser.getRoles().remove(role);

        if (removed) {
            userRepository.save(dbUser);

            auditService.log(auditService.success(
                    dbUser,
                    AuditAction.ROLE_REMOVED.name(),
                    "ROLE",
                    role.getId(),
                    getClientIp(request),
                    request.getHeader("User-Agent")
            ));

            log.info("Role {} removed from user {}", roleName, user.getId());
        }
    }

    // =========================================================
    // 🔧 ATTACH PERMISSIONS TO ROLE (ADMIN ONLY)
    // =========================================================
    public void attachPermissionsToRole(String roleName, Set<String> permissionNames,
                                        User adminUser, HttpServletRequest request) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        Set<Permission> permissionEntities = permissionNames.stream()
                .map(name -> permissionRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + name)))
                .collect(Collectors.toSet());

        role.setPermissions(permissionEntities);
        roleRepository.save(role);

        auditService.log(auditService.successWithChanges(
                adminUser,
                AuditAction.ROLE_UPDATED.name(),
                "ROLE",
                role.getId(),
                null,
                "Permissions: " + String.join(", ", permissionNames),
                getClientIp(request),
                request.getHeader("User-Agent")
        ));

        log.info("Permissions updated for role {}", roleName);
    }

    // =========================================================
    // ✅ CHECK IF USER HAS SPECIFIC ROLE
    // =========================================================
    public boolean hasRole(User user, String roleName) {
        return user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    // =========================================================
    // 🔍 GET ROLE WITH PERMISSIONS
    // =========================================================
    public Role getRoleWithPermissions(String roleName) {
        return roleRepository.findByNameWithPermissions(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    }

    // =========================================================
    // 📋 GET ALL ROLES
    // =========================================================
    public Set<Role> getAllRoles() {
        return Set.copyOf(roleRepository.findAll());
    }

    // =========================================================
    // 🆕 CREATE NEW ROLE (ADMIN ONLY)
    // =========================================================
    public Role createRole(String roleName, String description, User adminUser, HttpServletRequest request) {

        if (roleRepository.existsByName(roleName)) {
            throw new BaseException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Role already exists: " + roleName);
        }

        Role role = Role.builder()
                .name(roleName)
                .description(description)
                .permissions(Set.of())
                .build();

        Role savedRole = roleRepository.save(role);

        auditService.log(auditService.success(
                adminUser,
                AuditAction.ROLE_CREATED.name(),
                "ROLE",
                savedRole.getId(),
                getClientIp(request),
                request.getHeader("User-Agent")
        ));

        log.info("New role created: {}", roleName);

        return savedRole;
    }

    // =========================================================
    // 🌐 IP HELPER
    // =========================================================
    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0] : request.getRemoteAddr();
    }
}