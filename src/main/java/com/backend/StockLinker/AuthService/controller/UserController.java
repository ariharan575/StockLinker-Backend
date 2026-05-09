package com.backend.StockLinker.AuthService.controller;

import com.backend.StockLinker.AuthService.dto.response.UserInfoResponse;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final RoleService roleService;

    // =========================================================
    // 🎯    USER INFO (POST LOGIN)
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        String userId = auth.getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        boolean isNewUser = user.getRoles().isEmpty();

        return ResponseEntity.ok(UserInfoResponse.fromUser(isNewUser,user));
    }

    // =========================================================
    // 🎯 ROLE SELECTION (POST LOGIN)
    // =========================================================
    @PostMapping("/role/select")
    public ResponseEntity<?> selectRole(
            @RequestParam String role,
            Authentication auth
    ) {

        String userId = auth.getName();

        User user = userRepository.findById(userId)
                .orElseThrow();

        roleService.upgradeRole(user, role);

        userRepository.save(user);

        return ResponseEntity.ok("Role upgraded to " + role);
    }
}
