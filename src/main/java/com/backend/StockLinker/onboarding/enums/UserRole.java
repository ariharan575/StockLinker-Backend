package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents supported user roles.
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

    WHOLESALER("Wholesaler"),
    SHOPKEEPER("Shopkeeper");

    private final String displayName;
}