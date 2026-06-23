package com.backend.StockLinker.onboarding.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for onboarding module.
 */
@Configuration
@ComponentScan(
        basePackages = {
                "com.stocklinker.onboarding"
        }
)
public class OnboardingModuleConfiguration {
}
