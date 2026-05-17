package com.backend.StockLinker.AuthService.mapper;

import com.backend.StockLinker.AuthService.dto.response.UserInfoResponse;
import com.backend.StockLinker.AuthService.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    // =========================================================
    // 🔄 USER TO USER INFO RESPONSE
    // =========================================================
    public UserInfoResponse toUserInfoResponse(User user, boolean isNewUser) {
        if (user == null) return null;

        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .isNewUser(isNewUser)
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .accountLocked(user.isAccountLocked())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .roles(user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()))
                .permissions(user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(permission -> permission.getName())
                        .collect(Collectors.toSet()))
                .deviceCount(user.getDevices() != null ? user.getDevices().size() : 0)
                .build();
    }
}