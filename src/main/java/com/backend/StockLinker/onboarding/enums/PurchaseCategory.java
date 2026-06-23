package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents shopkeeper purchase categories.
 */
@Getter
@RequiredArgsConstructor
public enum PurchaseCategory {

    BEVERAGES("Beverages"),
    RICE_AND_GROCERY("Rice & Grocery"),
    FMCG("FMCG"),
    FRESH_PRODUCE("Fresh Produce"),
    MEDICAL("Medical"),
    ELECTRONICS("Electronics");

    private final String displayName;
}