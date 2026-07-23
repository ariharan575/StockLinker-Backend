package com.backend.StockLinker.MessageService.enums;

/**
 * Delivery lifecycle status of an individual Message.
 * Ordered so that ordinal comparisons (SENT < DELIVERED < READ) are meaningful
 * for guarding against status regressions (e.g. never downgrade READ -> DELIVERED).
 */
public enum MessageStatus {
    SENT,
    DELIVERED,
    READ
}
