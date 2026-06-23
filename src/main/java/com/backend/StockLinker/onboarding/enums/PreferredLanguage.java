package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents supported application languages.
 */
@Getter
@RequiredArgsConstructor
public enum PreferredLanguage {

    ENGLISH("English"),
    TAMIL("Tamil");

    private final String displayName;
}