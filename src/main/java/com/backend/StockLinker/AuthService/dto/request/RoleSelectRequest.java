package com.backend.StockLinker.AuthService.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RoleSelectRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(SHOPKEEPER|WHOLESALER)$",
            message = "Role must be either SHOPKEEPER or WHOLESALER")
    private String role;
}
