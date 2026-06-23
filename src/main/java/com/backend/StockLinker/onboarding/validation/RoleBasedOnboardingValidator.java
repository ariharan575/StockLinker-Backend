package com.backend.StockLinker.onboarding.validation;

import com.backend.StockLinker.onboarding.enums.UserRole;
import com.backend.StockLinker.onboarding.exception.InvalidRoleSetupException;
import org.springframework.stereotype.Component;

/**
 * Validator for role-based onboarding setup access.
 */
@Component
public class RoleBasedOnboardingValidator {

    /**
     * Validate wholesaler access.
     *
     * @param role authenticated role
     */
    public void validateWholesalerAccess(
            final UserRole role
    ) {

        if (role != UserRole.WHOLESALER) {

            throw new InvalidRoleSetupException(
                    "Only wholesalers can access this setup"
            );
        }
    }

    /**
     * Validate shopkeeper access.
     *
     * @param role authenticated role
     */
    public void validateShopkeeperAccess(
            final UserRole role
    ) {

        if (role != UserRole.SHOPKEEPER) {

            throw new InvalidRoleSetupException(
                    "Only shopkeepers can access this setup"
            );
        }
    }
}
