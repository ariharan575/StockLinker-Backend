package com.backend.StockLinker.AuthService.dto.response;

import com.backend.StockLinker.AuthService.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String userId;

    private String role;

    private AccountStatus accountStatus;
}