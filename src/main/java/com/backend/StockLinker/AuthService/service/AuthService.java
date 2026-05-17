package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.exception.customExceptions.AccountBlockedException;
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

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthFlowService authFlowService;

    // =========================================================
    // ✅ PHONE OTP LOGIN (FIREBASE)
    // =========================================================
    public AuthResponse loginWithPhoneOtp(
            String idToken,
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Validate input
        if (idToken == null || idToken.isBlank()) {
            throw new BadRequestException("ID token is required");
        }

        try {
            // Verify Firebase token
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);

            String phone = (String) decoded.getClaims().get("phone_number");
            String email = decoded.getEmail();
            String name = decoded.getName();
            String uid = decoded.getUid();

            // Validate phone number
            if (phone == null || phone.isBlank()) {
                throw new BadRequestException("Phone number not found in token");
            }

            // Find or create user
            User user = userRepository.findByPhone(phone)
                    .orElseGet(() -> {
                        User u = new User();
                        u.setPhone(phone);
                        u.setEmail(email);
                        u.setName(name != null ? name : "User");
                        u.setUniqueId(uid);
                        u.setProvider("FIREBASE");
                        return userRepository.save(u);
                    });

            // Check if account is blocked
            if (!user.isActive()) {
                throw new AccountBlockedException("Your account has been blocked. Please contact support.");
            }

            // Process login through auth flow
            return authFlowService.login(user, "FIREBASE", deviceId, request, response);

        } catch (AccountBlockedException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Phone login failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Firebase token: " + e.getMessage());
        }
    }

    // =========================================================
    // ✅ GUEST LOGIN
    // =========================================================
    public AuthResponse guestLogin(
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Create new guest user
        User guest = new User();
        guest.setName("Guest-" + UUID.randomUUID().toString().substring(0, 8));
        guest.setUniqueId(UUID.randomUUID().toString());
        guest.setProvider("GUEST");
        guest = userRepository.save(guest);

        log.info("New guest user created with id: {}", guest.getId());

        // Process login through auth flow
        return authFlowService.login(guest, "GUEST", deviceId, request, response);
    }
}