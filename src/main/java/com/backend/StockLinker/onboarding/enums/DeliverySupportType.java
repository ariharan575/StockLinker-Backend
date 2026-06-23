package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents delivery support availability.
 */
@Getter
@RequiredArgsConstructor
public enum DeliverySupportType {

    AVAILABLE("Available"),
    NOT_AVAILABLE("Not Available"),
    THIRD_PARTY("Third Party");

    private final String displayName;
}