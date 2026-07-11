package com.backend.StockLinker.AuthService.dto.response;

import com.backend.StockLinker.AuthService.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserInfoResponse {
    private String id;
    private String email;
    private String phone;
    private String name;
    private boolean isNewUser;
    private String avatarUrl;
    private String accountStatus;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private String roles;
    private int deviceCount;

    // =========================================================
    // 🏗️ FACTORY METHOD - FROM USER
    // =========================================================
    public static UserInfoResponse fromUser(boolean isNewUser, User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .isNewUser(isNewUser)
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .roles(user.getRole())
                .deviceCount(user.getDevices() != null ? user.getDevices().size() : 0)
                .build();
    }
}