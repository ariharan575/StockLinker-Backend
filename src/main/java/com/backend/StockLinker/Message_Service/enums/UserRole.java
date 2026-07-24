package com.backend.StockLinker.Message_Service.enums;

/**
 * Represents the role of a participant in a conversation.
 * Mirrors the role names issued as "ROLE_x" authorities by AuthService's JwtService.
 */
public enum UserRole {
    BUYER,
    SELLER
}