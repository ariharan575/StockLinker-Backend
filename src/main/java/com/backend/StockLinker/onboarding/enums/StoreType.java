package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents shopkeeper store classifications.
 */
@Getter
@RequiredArgsConstructor
public enum StoreType {

    SMALL_SHOP("Small Shop"),
    MEDIUM_SHOP("Medium Shop"),
    SUPERMARKET("Supermarket"),
    WHOLESALE_RETAIL("Wholesale Retail");

    private final String displayName;
}