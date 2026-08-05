package com.backend.StockLinker.Auth_Service.dto.response;

import com.backend.StockLinker.Auth_Service.enums.AccountStatus;
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