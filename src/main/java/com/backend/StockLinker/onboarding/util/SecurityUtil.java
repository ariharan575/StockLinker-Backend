package com.backend.StockLinker.onboarding.util;

import com.backend.StockLinker.onboarding.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security-related operations.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * Get authenticated user id.
     *
     * @return authenticated user id
     */
    public static Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUserPrincipal) {

            return customUserPrincipal.getUserId();
        }

        throw new IllegalStateException(
                "Unable to fetch authenticated user"
        );
    }

    /**
     * Get authenticated user role.
     *
     * @return authenticated user role
     */
    public static UserRole getCurrentUserRole() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUserPrincipal) {

            return customUserPrincipal.getRole();
        }

        throw new IllegalStateException(
                "Unable to fetch authenticated role"
        );
    }

    /**
     * Custom authenticated principal contract.
     */
    public interface CustomUserPrincipal {

        Long getUserId();

        UserRole getRole();
    }
}
