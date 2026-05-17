package com.backend.StockLinker.AuthService.config;

import com.backend.StockLinker.AuthService.model.Permission;
import com.backend.StockLinker.AuthService.model.Role;
import com.backend.StockLinker.AuthService.repository.PermissionRepository;
import com.backend.StockLinker.AuthService.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializePermissions();
        initializeRoles();
        assignPermissionsToRoles();
        log.info("✅ Data initialization completed!");
    }

    private void initializePermissions() {
        log.info("Initializing permissions...");

        // Business Permissions
        createPermissionIfNotExists("BUSINESS_CREATE", "BUSINESS", "CREATE", "Create business profile");
        createPermissionIfNotExists("BUSINESS_VIEW", "BUSINESS", "READ", "View business details");
        createPermissionIfNotExists("BUSINESS_UPDATE", "BUSINESS", "UPDATE", "Update business info");

        // Product Permissions
        createPermissionIfNotExists("PRODUCT_CREATE", "PRODUCT", "CREATE", "Add new product");
        createPermissionIfNotExists("PRODUCT_VIEW", "PRODUCT", "READ", "View product details");
        createPermissionIfNotExists("PRODUCT_UPDATE", "PRODUCT", "UPDATE", "Edit product");
        createPermissionIfNotExists("PRODUCT_DELETE", "PRODUCT", "DELETE", "Delete product");
        createPermissionIfNotExists("PRODUCT_VIEW_OWN", "PRODUCT", "READ_OWN", "View own products only");
        createPermissionIfNotExists("PRODUCT_UPDATE_OWN", "PRODUCT", "UPDATE_OWN", "Update own products only");
        createPermissionIfNotExists("CATEGORY_MANAGE", "PRODUCT", "CATEGORY", "Manage categories");
        createPermissionIfNotExists("PRODUCT_IMAGE_UPLOAD", "PRODUCT", "UPLOAD", "Upload product images");
        createPermissionIfNotExists("BULK_PRICING_MANAGE", "PRODUCT", "PRICING", "Manage bulk pricing");
        createPermissionIfNotExists("MOQ_MANAGE", "PRODUCT", "MOQ", "Set minimum order quantity");

        // Order Permissions
        createPermissionIfNotExists("ORDER_CREATE", "ORDER", "CREATE", "Place new order");
        createPermissionIfNotExists("ORDER_VIEW", "ORDER", "READ", "View order details");
        createPermissionIfNotExists("ORDER_VIEW_OWN", "ORDER", "READ_OWN", "View own orders");
        createPermissionIfNotExists("ORDER_CANCEL", "ORDER", "CANCEL", "Cancel order");
        createPermissionIfNotExists("ORDER_UPDATE_STATUS", "ORDER", "UPDATE", "Update order status");
        createPermissionIfNotExists("ORDER_ASSIGN", "ORDER", "ASSIGN", "Assign to delivery partner");
        createPermissionIfNotExists("ORDER_REPEAT", "ORDER", "REPEAT", "Reorder items");

        // Marketplace Permissions
        createPermissionIfNotExists("VIEW_SELLERS", "MARKETPLACE", "READ", "View sellers");
        createPermissionIfNotExists("SEARCH_PRODUCT", "MARKETPLACE", "SEARCH", "Search products");

        // Trust/Review Permissions
        createPermissionIfNotExists("REVIEW_CREATE", "TRUST", "CREATE", "Give review");
        createPermissionIfNotExists("REVIEW_VIEW", "TRUST", "READ", "View reviews");
        createPermissionIfNotExists("TRUST_SCORE_VIEW", "TRUST", "SCORE", "View trust score");

        // Admin Permissions
        createPermissionIfNotExists("ADMIN_ACCESS", "ADMIN", "ACCESS", "Access admin dashboard");
        createPermissionIfNotExists("USER_MANAGEMENT", "ADMIN", "USER", "Manage users");
        createPermissionIfNotExists("ORDER_MONITOR", "ADMIN", "ORDER", "Monitor all orders");
        createPermissionIfNotExists("SYSTEM_CONFIG", "ADMIN", "CONFIG", "System configuration");
        createPermissionIfNotExists("ROLE_MANAGEMENT", "ADMIN", "ROLE", "Manage roles");
        createPermissionIfNotExists("PERMISSION_MANAGEMENT", "ADMIN", "PERMISSION", "Manage permissions");
    }

    private void createPermissionIfNotExists(String name, String resource, String action, String description) {
        if (!permissionRepository.existsByName(name)) {
            Permission permission = Permission.builder()
                    .name(name)
                    .resource(resource)
                    .action(action)
                    .description(description)
                    .build();
            permissionRepository.save(permission);
            log.debug("Created permission: {}", name);
        }
    }

    private void initializeRoles() {
        log.info("Initializing roles...");

        // ADMIN ROLE
        if (!roleRepository.existsByName("ADMIN")) {
            Role admin = Role.builder()
                    .name("ADMIN")
                    .description("Full system access")
                    .permissions(new HashSet<>())  // ✅ Mutable HashSet
                    .build();
            roleRepository.save(admin);
            log.info("Created ADMIN role");
        }

        // SHOPKEEPER ROLE
        if (!roleRepository.existsByName("SHOPKEEPER")) {
            Role shopkeeper = Role.builder()
                    .name("SHOPKEEPER")
                    .description("Shop owner - can manage products, orders, business")
                    .permissions(new HashSet<>())  // ✅ Mutable HashSet
                    .build();
            roleRepository.save(shopkeeper);
            log.info("Created SHOPKEEPER role");
        }

        // WHOLESALER ROLE
        if (!roleRepository.existsByName("WHOLESALER")) {
            Role wholesaler = Role.builder()
                    .name("WHOLESALER")
                    .description("Wholesaler - can manage bulk products and pricing")
                    .permissions(new HashSet<>())  // ✅ Mutable HashSet
                    .build();
            roleRepository.save(wholesaler);
            log.info("Created WHOLESALER role");
        }

        // GUEST ROLE
        if (!roleRepository.existsByName("GUEST")) {
            Role guest = Role.builder()
                    .name("GUEST")
                    .description("Basic read-only access")
                    .permissions(new HashSet<>())  // ✅ Mutable HashSet
                    .build();
            roleRepository.save(guest);
            log.info("Created GUEST role");
        }
    }

    private void assignPermissionsToRoles() {
        log.info("Assigning permissions to roles...");

        // ADMIN - All permissions (using mutable HashSet)
        Role admin = roleRepository.findByName("ADMIN").orElse(null);
        if (admin != null) {
            Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());  // ✅ Mutable HashSet
            admin.setPermissions(allPermissions);
            roleRepository.save(admin);
            log.info("Assigned {} permissions to ADMIN", allPermissions.size());
        }

        // SHOPKEEPER - Full product, order, business, marketplace permissions
        Role shopkeeper = roleRepository.findByName("SHOPKEEPER").orElse(null);
        if (shopkeeper != null) {
            Set<Permission> shopkeeperPermissions = new HashSet<>();  // ✅ Mutable HashSet
            addPermissionIfExists(shopkeeperPermissions, "BUSINESS_CREATE");
            addPermissionIfExists(shopkeeperPermissions, "BUSINESS_VIEW");
            addPermissionIfExists(shopkeeperPermissions, "BUSINESS_UPDATE");
            addPermissionIfExists(shopkeeperPermissions, "PRODUCT_CREATE");
            addPermissionIfExists(shopkeeperPermissions, "PRODUCT_VIEW");
            addPermissionIfExists(shopkeeperPermissions, "PRODUCT_UPDATE");
            addPermissionIfExists(shopkeeperPermissions, "PRODUCT_DELETE");
            addPermissionIfExists(shopkeeperPermissions, "CATEGORY_MANAGE");
            addPermissionIfExists(shopkeeperPermissions, "PRODUCT_IMAGE_UPLOAD");
            addPermissionIfExists(shopkeeperPermissions, "BULK_PRICING_MANAGE");
            addPermissionIfExists(shopkeeperPermissions, "MOQ_MANAGE");
            addPermissionIfExists(shopkeeperPermissions, "ORDER_CREATE");
            addPermissionIfExists(shopkeeperPermissions, "ORDER_VIEW");
            addPermissionIfExists(shopkeeperPermissions, "ORDER_CANCEL");
            addPermissionIfExists(shopkeeperPermissions, "ORDER_UPDATE_STATUS");
            addPermissionIfExists(shopkeeperPermissions, "ORDER_REPEAT");
            addPermissionIfExists(shopkeeperPermissions, "VIEW_SELLERS");
            addPermissionIfExists(shopkeeperPermissions, "SEARCH_PRODUCT");
            addPermissionIfExists(shopkeeperPermissions, "REVIEW_CREATE");
            addPermissionIfExists(shopkeeperPermissions, "REVIEW_VIEW");
            addPermissionIfExists(shopkeeperPermissions, "TRUST_SCORE_VIEW");

            shopkeeper.setPermissions(shopkeeperPermissions);
            roleRepository.save(shopkeeper);
            log.info("Assigned {} permissions to SHOPKEEPER", shopkeeperPermissions.size());
        }

        // WHOLESALER - Limited to own product management
        Role wholesaler = roleRepository.findByName("WHOLESALER").orElse(null);
        if (wholesaler != null) {
            Set<Permission> wholesalerPermissions = new HashSet<>();  // ✅ Mutable HashSet
            addPermissionIfExists(wholesalerPermissions, "BUSINESS_VIEW");
            addPermissionIfExists(wholesalerPermissions, "PRODUCT_VIEW_OWN");
            addPermissionIfExists(wholesalerPermissions, "PRODUCT_UPDATE_OWN");
            addPermissionIfExists(wholesalerPermissions, "BULK_PRICING_MANAGE");
            addPermissionIfExists(wholesalerPermissions, "MOQ_MANAGE");
            addPermissionIfExists(wholesalerPermissions, "ORDER_VIEW_OWN");
            addPermissionIfExists(wholesalerPermissions, "VIEW_SELLERS");
            addPermissionIfExists(wholesalerPermissions, "SEARCH_PRODUCT");
            addPermissionIfExists(wholesalerPermissions, "REVIEW_VIEW");

            wholesaler.setPermissions(wholesalerPermissions);
            roleRepository.save(wholesaler);
            log.info("Assigned {} permissions to WHOLESALER", wholesalerPermissions.size());
        }

        // GUEST - Read-only access
        Role guest = roleRepository.findByName("GUEST").orElse(null);
        if (guest != null) {
            Set<Permission> guestPermissions = new HashSet<>();  // ✅ Mutable HashSet
            addPermissionIfExists(guestPermissions, "PRODUCT_VIEW");
            addPermissionIfExists(guestPermissions, "VIEW_SELLERS");
            addPermissionIfExists(guestPermissions, "SEARCH_PRODUCT");
            addPermissionIfExists(guestPermissions, "REVIEW_VIEW");
            addPermissionIfExists(guestPermissions, "TRUST_SCORE_VIEW");

            guest.setPermissions(guestPermissions);
            roleRepository.save(guest);
            log.info("Assigned {} permissions to GUEST", guestPermissions.size());
        }
    }

    private void addPermissionIfExists(Set<Permission> permissions, String permissionName) {
        permissionRepository.findByName(permissionName).ifPresent(permissions::add);
    }
}