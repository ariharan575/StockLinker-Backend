package com.backend.StockLinker.AuthService.dto.response;

import com.backend.StockLinker.AuthService.enums.AccountStatus;
import com.backend.StockLinker.AuthService.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String role;
    private AccountStatus accountStatus;
    private String nextStep;

    // =========================================================
    // 🏗️ FACTORY METHOD - FROM USER
    // =========================================================
    public static AuthResponse fromUser(String accessToken,
                                        String refreshToken,
                                        User user   ,
                                         String nextStep) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .nextStep(nextStep)
                .build();
    }

    // =========================================================
    // 🏗️ FACTORY METHOD - SIMPLE
    // =========================================================
    public static AuthResponse simple(String accessToken, String refreshToken, String userId) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .build();
    }
}