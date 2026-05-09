package com.backend.StockLinker.AuthService.dto.response;

import com.backend.StockLinker.AuthService.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

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
    private boolean accountLocked;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Set<String> roles;
    private Set<String> permissions;

    public static UserInfoResponse fromUser(boolean isNewUser, User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .isNewUser(isNewUser)
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus().name())
                .accountLocked(user.isAccountLocked())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .roles(user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()))
                .permissions(user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(perm -> perm.getName())
                        .collect(Collectors.toSet()))
                .build();
    }
}

