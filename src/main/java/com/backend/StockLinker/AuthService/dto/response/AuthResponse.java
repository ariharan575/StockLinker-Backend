package com.backend.StockLinker.AuthService.dto.response;

import com.backend.StockLinker.AuthService.model.Role;
import com.backend.StockLinker.AuthService.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;
    private String userId;
    private Set<Role> roles;


    public static AuthResponse fromUser (boolean isNewUser,String accessToken,String refreshToken, User user){
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .userId(user.getId())
                .roles(user.getRoles()).build();
    }
}