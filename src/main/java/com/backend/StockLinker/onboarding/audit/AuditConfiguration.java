package com.backend.StockLinker.onboarding.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class for JPA auditing.
 */
@Configuration
@EnableJpaAuditing(
        auditorAwareRef = "auditAwareImpl"
)
public class AuditConfiguration {
}
