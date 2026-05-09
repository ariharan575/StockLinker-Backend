package com.backend.StockLinker.AuthService.dto.request;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken; //  will also read from cookie
}
