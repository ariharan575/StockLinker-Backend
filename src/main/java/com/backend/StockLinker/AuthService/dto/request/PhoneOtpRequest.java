package com.backend.StockLinker.AuthService.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PhoneOtpRequest {

    @NotBlank(message = "ID token is required and cannot be empty")
    @Pattern(
            regexp = "^[A-Za-z0-9\\-._~+/]+=*$",
            message = "Invalid ID token format. Token must be a valid JWT or Firebase ID token"
    )
    private String idToken;
}