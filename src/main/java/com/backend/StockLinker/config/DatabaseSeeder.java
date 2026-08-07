package com.backend.StockLinker.config;

import com.backend.StockLinker.Auth_Service.model.Role;
import com.backend.StockLinker.Auth_Service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedRoles();
    }

    private void seedRoles() {
        // 1. ADMIN
        createRoleIfNotExists("ADMIN", "System Administrator");

        // 2. WHOLESALER
        createRoleIfNotExists("WHOLESALER", "Wholesaler Account");

        // 3. SHOPKEEPER
        createRoleIfNotExists("SHOPKEEPER", "Shopkeeper Account");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = Role.builder()
                    .name(roleName)
                    .description(description)
                    .build();

            roleRepository.save(role);
        }
    }
}