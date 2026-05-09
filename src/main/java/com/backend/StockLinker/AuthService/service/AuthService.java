package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.RoleRepository;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthFlowService authFlowService;
    private final RoleRepository roleRepository;

    // =========================================================
    // ✅ PHONE OTP LOGIN (FIREBASE)
    // =========================================================
    public Map<String, Object> loginWithPhoneOtp(
            String idToken,
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        try {
            // 🔐 VERIFY FIREBASE TOKEN
            FirebaseToken decoded =
                    FirebaseAuth.getInstance().verifyIdToken(idToken);

            String phone = (String) decoded.getClaims().get("phone_number");
            String email = decoded.getEmail();
            String name = decoded.getName();
            String uid = decoded.getUid();

            if (phone == null) {
                throw new BaseException(ErrorCode.BAD_REQUEST, "Phone not found in token");
            }

            // =====================================================
            // ✅ FIND OR CREATE USER
            // =====================================================
            User user = userRepository.findByPhone(phone)
                    .orElseGet(() -> {
                        User u = new User();
                        u.setPhone(phone);
                        u.setEmail(email);
                        u.setName(name);
                        u.setUniqueId(uid);
                        u.setProvider("FIREBASE");
                        return userRepository.save(u);
                    });

            // =====================================================
            // 🔒 ACCOUNT VALIDATION
            // =====================================================
            if (!user.isActive()) {
                throw new BaseException(ErrorCode.ACCOUNT_BLOCKED);
            }

            // =====================================================
            // 🔥 MAIN AUTH FLOW (IMPORTANT)
            // =====================================================
            return authFlowService.login(
                    user,
                    "FIREBASE",
                    deviceId,
                    request,
                    response
            );

        } catch (Exception e) {
            log.error("Phone login failed: {}", e.getMessage());

            throw new BaseException(
                    ErrorCode.UNAUTHORIZED,
                    "Invalid Firebase token"
            );
        }
    }


    // =========================================================
    // ✅ GUEST LOGIN
    // =========================================================
    public Map<String, Object> guestLogin(
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        User guest = new User();

        guest.setName("Guest-" + UUID.randomUUID().toString().substring(0,8));
        guest.setUniqueId(UUID.randomUUID().toString());
        guest.setProvider("GUEST");

        guest = userRepository.save(guest);

        return authFlowService.login(
                guest,
                "GUEST",
                deviceId,
                request,
                response
        );
    }
}