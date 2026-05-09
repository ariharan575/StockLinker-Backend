package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.model.Permission;
import com.backend.StockLinker.AuthService.model.Role;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.PermissionRepository;
import com.backend.StockLinker.AuthService.repository.RoleRepository;
import com.backend.StockLinker.AuthService.repository.UserRepository;
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

    // ✅ ADD THIS MISSING METHOD
    public boolean hasBusinessRole(User user) {
        return user.getRoles()
                .stream()
                .anyMatch(r -> r.getName().equals("SHOPKEEPER") || r.getName().equals("WHOLESALER"));
    }

    // ✅ ADD THIS MISSING METHOD
    public String getBusinessRole(User user) {
        return user.getRoles()
                .stream()
                .filter(r -> r.getName().equals("SHOPKEEPER") || r.getName().equals("WHOLESALER"))
                .map(Role::getName)
                .findFirst()
                .orElse(null);
    }

    // ✅ ADD THIS MISSING METHOD
    public Set<String> getUserRoles(User user) {
        return user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    // =========================================================
    // ✅ ASSIGN BASE ROLE (AFTER LOGIN)
    // =========================================================
    public void assignGuestRole(User user) {  // ✅ RENAMED from assignUserRoleIfNotExists
        Role guestRole = roleRepository.findByName("GUEST")
                .orElseThrow(() -> new RuntimeException("Role GUEST not found"));

        User dbUser = userRepository.findById(user.getId())
                .orElseThrow();

        boolean alreadyExists = dbUser.getRoles()
                .stream()
                .anyMatch(r -> r.getId().equals(guestRole.getId()));

        if (!alreadyExists) {
            dbUser.getRoles().add(guestRole);
            userRepository.save(dbUser);
            log.info("GUEST role assigned: {}", dbUser.getId());
        }
    }

    // =========================================================
    // ✅ ROLE UPGRADE (USER → SHOPKEEPER / WHOLESALER)
    // =========================================================
    public void upgradeRole(User user, String roleName) {

        // ✅ VALIDATE ROLE NAME
        if (!roleName.equals("SHOPKEEPER") && !roleName.equals("WHOLESALER")) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Invalid role. Allowed: SHOPKEEPER, WHOLESALER");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BaseException(ErrorCode.BAD_REQUEST, "Role not found: " + roleName));

        User dbUser = userRepository.findById(user.getId())
                .orElseThrow();

        // Check if user already has a business role
        if (hasBusinessRole(dbUser)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "User already has a business role");
        }

        // Prevent duplicate role
        boolean alreadyExists = dbUser.getRoles()
                .stream()
                .anyMatch(r -> r.getName().equals(roleName));

        if (!alreadyExists) {
            dbUser.getRoles().add(role);
            userRepository.save(dbUser);
            log.info("User {} upgraded to role {}", user.getId(), roleName);
        }
    }

    // =========================================================
    // ✅ REMOVE ROLE (OPTIONAL - ADMIN CONTROL)
    // =========================================================
    public void removeRole(User user, String roleName) {

        user.getRoles().removeIf(role -> role.getName().equals(roleName));

        log.info("Role {} removed from user {}", roleName, user.getId());
    }

    // =========================================================
    // ✅ SET ROLE PERMISSIONS (ADMIN CONFIG)
    // =========================================================
    public void attachPermissions(String roleName, Set<String> permissions) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.BAD_REQUEST, "Role not found: " + roleName));

        Set<Permission> permissionEntities = permissions.stream()
                .map(name -> permissionRepository.findByName(name)
                        .orElseThrow(() ->
                                new BaseException(ErrorCode.BAD_REQUEST,
                                        "Permission not found: " + name)))
                .collect(Collectors.toSet());

        role.setPermissions(permissionEntities);

        log.info("Permissions updated for role {}", roleName);
    }

    // =========================================================
    // ✅ GET USER ROLES (FOR DEBUG / RESPONSE)
    // =========================================================
    public Set<String> getRoleNames(User user) {

        return user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    // =========================================================
    // ✅ CHECK ROLE EXISTS (SECURITY USE)
    // =========================================================
    public boolean hasRole(User user, String roleName) {

        return user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
}