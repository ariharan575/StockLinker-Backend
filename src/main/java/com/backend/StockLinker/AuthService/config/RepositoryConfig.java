package com.backend.StockLinker.AuthService.config; // Adjust package if this file is located elsewhere

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.backend.StockLinker.ProfileService.repository.postgres",
        "com.backend.StockLinker.AuthService.repository" // <-- ADDED: Now Spring will find AuditLogRepository
})
//@EnableMongoRepositories(basePackages = {
//        "com.backend.StockLinker.ProfileService.repository.mongo"
//})
public class RepositoryConfig {
}