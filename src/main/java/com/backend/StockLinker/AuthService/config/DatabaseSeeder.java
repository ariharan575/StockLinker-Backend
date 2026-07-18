package com.backend.StockLinker.AuthService.config;

import com.backend.StockLinker.AuthService.model.Permission;
import com.backend.StockLinker.AuthService.model.Role;
import com.backend.StockLinker.AuthService.repository.PermissionRepository;
import com.backend.StockLinker.AuthService.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedPermissions();
        seedRoles();
    }

    private void seedPermissions() {
        List<String[]> permissions = Arrays.asList(
                new String[]{"PRODUCT_VIEW", "View Products"},
                new String[]{"PRODUCT_CREATE", "Create Product"},
                new String[]{"PRODUCT_UPDATE", "Update Product"},
                new String[]{"PRODUCT_DELETE", "Delete Product"},
                new String[]{"ORDER_CREATE", "Create Order"},
                new String[]{"ORDER_VIEW", "View Orders"},
                new String[]{"ORDER_UPDATE", "Update Order"},
                new String[]{"ORDER_CANCEL", "Cancel Order"},
                new String[]{"PRICE_COMPARE", "Compare Prices"},
                new String[]{"NEARBY_SUPPLIER", "Nearby Suppliers"},
                new String[]{"PROFILE_UPDATE", "Update Profile"},
                new String[]{"BUSINESS_UPDATE", "Update Business"},
                new String[]{"USER_MANAGE", "Manage Users"},
                new String[]{"DASHBOARD_VIEW", "Dashboard Access"}
        );

        for (String[] permData : permissions) {
            String name = permData[0];
            String description = permData[1];
            if (permissionRepository.findByName(name).isEmpty()) {
                Permission permission = Permission.builder()
                        .name(name)
                        .description(description)
                        .build();
                permissionRepository.save(permission);
            }
        }
    }

    private void seedRoles() {
        // 1. ADMIN
        createRoleIfNotExists("ADMIN", "System Administrator", Arrays.asList(
                "PRODUCT_VIEW", "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE",
                "ORDER_CREATE", "ORDER_VIEW", "ORDER_UPDATE", "ORDER_CANCEL",
                "PRICE_COMPARE", "NEARBY_SUPPLIER", "PROFILE_UPDATE", "BUSINESS_UPDATE",
                "USER_MANAGE", "DASHBOARD_VIEW"
        ));

        // 2. WHOLESALER
        createRoleIfNotExists("WHOLESALER", "Wholesaler Account", Arrays.asList(
                "PRODUCT_VIEW", "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE",
                "ORDER_VIEW", "ORDER_UPDATE", "PROFILE_UPDATE", "BUSINESS_UPDATE",
                "DASHBOARD_VIEW"
        ));

        // 3. SHOPKEEPER
        createRoleIfNotExists("SHOPKEEPER", "Shopkeeper Account", Arrays.asList(
                "PRODUCT_VIEW", "ORDER_CREATE", "ORDER_VIEW", "ORDER_CANCEL",
                "PRICE_COMPARE", "NEARBY_SUPPLIER", "PROFILE_UPDATE", "DASHBOARD_VIEW"
        ));

        // 4. GUEST
        createRoleIfNotExists("GUEST", "Guest Demo Account", Arrays.asList(
                "PRODUCT_VIEW", "PRICE_COMPARE", "NEARBY_SUPPLIER", "DASHBOARD_VIEW"
        ));
    }

    private void createRoleIfNotExists(String roleName, String description, List<String> permissionNames) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Set<Permission> rolePermissions = new HashSet<>();

            for (String pName : permissionNames) {
                permissionRepository.findByName(pName).ifPresent(rolePermissions::add);
            }

            Role role = Role.builder()
                    .name(roleName)
                    .description(description)
                    .permissions(rolePermissions)
                    .build();

            roleRepository.save(role);
        }
    }
}