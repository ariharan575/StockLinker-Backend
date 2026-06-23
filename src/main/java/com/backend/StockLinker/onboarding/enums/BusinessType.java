package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents supported business types.
 */
@Getter
@RequiredArgsConstructor
public enum BusinessType {

    RETAIL_SHOP("Retail Shop"),
    WHOLESALE_STORE("Wholesale Store"),
    DISTRIBUTOR("Distributor"),
    SUPERMARKET("Supermarket"),
    PHARMACY("Pharmacy"),
    FMCG_DEALER("FMCG Dealer"),
    ELECTRONICS_STORE("Electronics Store"),
    VEGETABLE_STORE("Vegetable Store");

    private final String displayName;
}