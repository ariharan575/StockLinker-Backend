package com.backend.StockLinker.AuthService.dto.response;

import com.backend.StockLinker.AuthService.model.Role;
import com.backend.StockLinker.AuthService.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;
    private boolean needsRoleSelection;
    private boolean hasBusinessRole;
    private String userId;
    private Set<String> roles;
    private Set<String> permissions;

    // =========================================================
    // 🏗️ FACTORY METHOD - FROM USER
    // =========================================================
    public static AuthResponse fromUser(boolean isNewUser,
                                        boolean needsRoleSelection,
                                        boolean hasBusinessRole,
                                        String accessToken,
                                        String refreshToken,
                                        User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .needsRoleSelection(needsRoleSelection)
                .hasBusinessRole(hasBusinessRole)
                .userId(user.getId())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .permissions(user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(permission -> permission.getName())
                        .collect(Collectors.toSet()))
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