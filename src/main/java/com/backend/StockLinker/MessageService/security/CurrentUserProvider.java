package com.backend.StockLinker.MessageService.security;

import com.backend.StockLinker.MessageService.enums.UserRole;
import com.backend.StockLinker.AuthService.exception.customExceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for "who is the current user" across MessageService.
 * Never trust a senderId/userId supplied by the frontend — always resolve through here.
 */
@Component
public class CurrentUserProvider {

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null
                || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found in security context");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String userId) || userId.isBlank()) {
            throw new UnauthorizedException("Invalid authentication principal");
        }
        return userId;
    }

    public UserRole getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("No authenticated user found in security context");
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .map(this::toUserRole)
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("No role found for current user"));
    }

    // 🚀 THE FIX: Map system roles (WHOLESALER/SHOPKEEPER) to chat roles (SELLER/BUYER)
    private UserRole toUserRole(String roleName) {
        String upperRole = roleName.toUpperCase();

        if (upperRole.equals("WHOLESALER")) {
            return UserRole.SELLER;
        }
        if (upperRole.equals("SHOPKEEPER")) {
            return UserRole.BUYER;
        }

        try {
            // Fallback for explicitly passed BUYER/SELLER roles if they exist
            return UserRole.valueOf(upperRole);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Unrecognized role: " + roleName);
        }
    }
}