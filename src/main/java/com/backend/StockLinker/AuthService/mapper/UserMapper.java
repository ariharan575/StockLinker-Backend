package com.backend.StockLinker.AuthService.mapper;

import com.backend.StockLinker.AuthService.dto.response.UserInfoResponse;
import com.backend.StockLinker.AuthService.model.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper {

    public UserInfoResponse toUserInfoResponse(User user, boolean isNewUser) {

        if (user == null) {
            return null;
        }

        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .isNewUser(isNewUser)
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(
                        user.getAccountStatus() != null
                                ? user.getAccountStatus().name()
                                : null
                )
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .roles(user.getRole())
                .deviceCount(
                        user.getDevices() != null
                                ? user.getDevices().size()
                                : 0
                )
                .build();
    }
}