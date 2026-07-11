package com.backend.StockLinker.AuthService.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneOtpRequest {
    
    @NotBlank(message = "ID token is required")
    private String idToken;
}