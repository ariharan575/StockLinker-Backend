package com.backend.StockLinker.Auth_Service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RoleSelectRequest {

    @NotBlank(message = "Role selection is required")
    @Pattern(
            regexp = "^(SHOPKEEPER|WHOLESALER)$",
            message = "Invalid role. Allowed roles: SHOPKEEPER, WHOLESALER"
    )
    private String role;
}