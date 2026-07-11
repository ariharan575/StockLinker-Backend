package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.enums.AccountStatus;
import com.backend.StockLinker.AuthService.enums.LoginProvider;
import com.backend.StockLinker.AuthService.exception.customExceptions.BadRequestException;
import com.backend.StockLinker.AuthService.exception.customExceptions.UnauthorizedException;
import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthFlowService authFlowService;

    // =========================================================
    // ✅ GOOGLE OAUTH LOGIN (DELEGATED FROM SUCCESS HANDLER)
    // =========================================================
    @Transactional
    public AuthResponse loginWithGoogle(
            String email,
            String name,
            String avatarUrl,
            String googleId,
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Find or create user enforcing the enterprise state machine
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setName(name != null ? name : "Google User");
                    u.setAvatarUrl(avatarUrl);
                    u.setProvider("GOOGLE");
                    u.setUniqueId(googleId);
                    u.setRole(null); // Explicitly null per specs
                    u.setAccountStatus(AccountStatus.PENDING_ROLE);
                    return userRepository.save(u);
                });

        return authFlowService.login(user, LoginProvider.GOOGLE, deviceId, request, response);
    }

    // =========================================================
    // ✅ PHONE OTP LOGIN (FIREBASE)
    // =========================================================
    @Transactional
    public AuthResponse loginWithPhoneOtp(
            String idToken,
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (idToken == null || idToken.isBlank()) {
            throw new BadRequestException("ID token is required");
        }

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String phone = (String) decoded.getClaims().get("phone_number");
            String email = decoded.getEmail();
            String name = decoded.getName();
            String uid = decoded.getUid();

            if (phone == null || phone.isBlank()) {
                throw new BadRequestException("Phone number not found in token");
            }

            // Find or create user enforcing the enterprise state machine
            User user = userRepository.findByPhone(phone)
                    .orElseGet(() -> {
                        User u = new User();
                        u.setPhone(phone);
                        u.setEmail(email);
                        u.setName(name != null ? name : "User");
                        u.setUniqueId(uid);
                        u.setProvider("FIREBASE");
                        u.setRole(null); // Explicitly null per specs
                        u.setAccountStatus(AccountStatus.PENDING_ROLE);
                        return userRepository.save(u);
                    });

            return authFlowService.login(user, LoginProvider.PHONE_OTP, deviceId, request, response);

        } catch ( BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Phone login failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Firebase token: " + e.getMessage());
        }
    }

    // =========================================================
    // ✅ GUEST LOGIN
    // =========================================================
    @Transactional
    public AuthResponse guestLogin(
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User guest = new User();
        guest.setName("Guest-" + UUID.randomUUID().toString().substring(0, 8));
        guest.setUniqueId(UUID.randomUUID().toString());
        guest.setProvider("GUEST");
        guest.setRole("GUEST"); // Bypass onboarding
        guest.setAccountStatus(AccountStatus.ACTIVE); // Immediate dashboard access

        guest = userRepository.save(guest);
        log.info("New guest user created with id: {}", guest.getId());

        return authFlowService.login(guest, LoginProvider.GUEST, deviceId, request, response);
    }
}