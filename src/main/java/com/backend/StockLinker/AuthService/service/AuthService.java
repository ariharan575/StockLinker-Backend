package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.enums.*;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.exception.customExceptions.*;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.RefreshToken;
import com.backend.StockLinker.AuthService.model.Role;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.model.UserDevice;
import com.backend.StockLinker.AuthService.repository.RoleRepository;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.repository.UserDeviceRepository;
import com.backend.StockLinker.AuthService.security.RefreshTokenService;
import com.backend.StockLinker.AuthService.security.TokenService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final AuthFlowService authFlowService;
    private final RefreshTokenService refreshTokenService;
    private final DeviceSessionService deviceSessionService;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    // ==========================================
    // 1. GOOGLE OAUTH LOGIN - FIXED
    // ==========================================
    @Transactional
    public AuthResponse googleLogin(OAuth2User oauthUser, String deviceId,
                                    HttpServletRequest request, HttpServletResponse response) {
        try {
            if (oauthUser == null) {
                throw new UnauthorizedException("OAuth2 user information is missing");
            }

            String email = oauthUser.getAttribute("email");

            if (email == null || email.isBlank()) {
                throw new UnauthorizedException("Email is required for Google login");
            }

            // ✅ FIX: Ensure deviceId is not null
            if (deviceId == null || deviceId.isBlank()) {
                log.warn("Device ID is null for Google login, generating new one");
                deviceId = UUID.randomUUID().toString();
                if (request != null) {
                    request.setAttribute("deviceId", deviceId);
                }
            }

            log.info("Processing Google login for email: {} with deviceId: {}", email, deviceId);

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                try {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(oauthUser.getAttribute("name"));
                    newUser.setAvatarUrl(oauthUser.getAttribute("picture"));
                    newUser.setProvider(Provider.GOOGLE);
                    newUser.setUniqueId(oauthUser.getAttribute("sub"));
                    newUser.setRole(null);
                    newUser.setAccountStatus(AccountStatus.PENDING_ROLE);
                    return userRepository.save(newUser);
                } catch (DataIntegrityViolationException e) {
                    log.error("Failed to create user: {}", e.getMessage());
                    throw new ConflictException("User already exists with this email");
                }
            });

            // ✅ FIX: Ensure we process login with the correct deviceId
            return authFlowService.processLogin(user, Provider.GOOGLE.name(), deviceId, request, response);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage(), e);
            throw new BaseException(ErrorCode.OAUTH_FAILED, "Google authentication failed: " + e.getMessage());
        }
    }

    // ==========================================
    // 2. FIREBASE PHONE OTP LOGIN
    // ==========================================
    @Transactional
    public AuthResponse phoneLogin(String idToken, String deviceId,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            if (idToken == null || idToken.isBlank()) {
                throw new BadRequestException("ID token is required for phone login");
            }

            if (deviceId == null || deviceId.isBlank()) {
                throw new BadRequestException("Device ID is required for phone login");
            }

            log.debug("Processing phone login for device: {}", deviceId);

            FirebaseToken decoded;
            try {
                decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            } catch (FirebaseAuthException e) {
                log.error("Firebase token verification failed: {}", e.getMessage());
                throw new InvalidTokenException("Invalid or expired Firebase token: " + e.getMessage());
            }

            String phone = (String) decoded.getClaims().get("phone_number");

            if (phone == null || phone.isBlank()) {
                throw new BadRequestException("Phone number missing in Firebase token");
            }

            User user = userRepository.findByPhone(phone).orElseGet(() -> {
                try {
                    User newUser = new User();
                    newUser.setPhone(phone);
                    newUser.setEmail(decoded.getEmail());
                    newUser.setName(decoded.getName() != null ? decoded.getName() : "User");
                    newUser.setUniqueId(decoded.getUid());
                    newUser.setProvider(Provider.PHONE_OTP);
                    newUser.setRole(null);
                    newUser.setAccountStatus(AccountStatus.PENDING_ROLE);
                    return userRepository.save(newUser);
                } catch (DataIntegrityViolationException e) {
                    log.error("Failed to create user: {}", e.getMessage());
                    throw new ConflictException("User already exists with this phone number");
                }
            });

            return authFlowService.processLogin(user, Provider.PHONE_OTP.name(), deviceId, request, response);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Phone login failed: {}", e.getMessage(), e);
            throw new BaseException(ErrorCode.UNAUTHORIZED, "Phone authentication failed: " + e.getMessage());
        }
    }

    // ==========================================
    // 3. GUEST LOGIN - FIXED
    // ==========================================
    @Transactional
    public AuthResponse guestLogin(String deviceId, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (deviceId == null || deviceId.isBlank()) {
                throw new BadRequestException("Device ID is required for guest login");
            }

            log.info("Processing guest login for device: {}", deviceId);

            // ✅ FIX: Check if device exists and is associated with a guest user
            UserDevice existingDevice = userDeviceRepository.findByDeviceId(deviceId).orElse(null);

            if (existingDevice != null) {
                User existingUser = existingDevice.getUser();
                if (existingUser.getProvider() == Provider.GUEST) {
                    log.info("Existing guest found: {}", existingUser.getId());
                    return authFlowService.processLogin(existingUser, Provider.GUEST.name(), deviceId, request, response);
                } else {
                    // ✅ FIX: Device belongs to a non-guest user - create new guest with new device ID
                    log.warn("Device belongs to non-guest user {}. Creating new guest with new device ID.", existingUser.getId());
                    // Generate new deviceId for guest
                    deviceId = UUID.randomUUID().toString();
                    if (request != null) {
                        request.setAttribute("deviceId", deviceId);
                    }
                }
            }

            Role guestRole = roleRepository.findByName(RoleConstants.GUEST)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Default GUEST role is not configured. Please contact administrator."
                    ));

            User guest = new User();
            guest.setName("Guest-" + UUID.randomUUID().toString().substring(0, 8));
            guest.setUniqueId(UUID.randomUUID().toString());
            guest.setProvider(Provider.GUEST);
            guest.setRole(guestRole);

            try {
                guest = userRepository.save(guest);
            } catch (DataIntegrityViolationException e) {
                log.error("Failed to create guest user: {}", e.getMessage());
                throw new ConflictException("Failed to create guest account");
            }

            auditService.log(AuditLogRequest.builder()
                    .userId(guest.getId())
                    .action(AuditAction.GUEST_LOGIN)
                    .resourceType(ResourceType.USER)
                    .resourceId(guest.getId())
                    .ipAddress(ipAddressService.getClientIp(request))
                    .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                    .deviceId(deviceId)
                    .status(AuditLog.Status.SUCCESS)
                    .build());

            return authFlowService.processLogin(guest, Provider.GUEST.name(), deviceId, request, response);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Guest login failed: {}", e.getMessage(), e);
            auditService.log(AuditLogRequest.builder()
                    .userId("UNKNOWN")
                    .action(AuditAction.GUEST_LOGIN)
                    .resourceType(ResourceType.USER)
                    .ipAddress(ipAddressService.getClientIp(request))
                    .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                    .deviceId(deviceId)
                    .status(AuditLog.Status.FAILURE)
                    .failureReason("Guest account creation failed: " + e.getMessage())
                    .build());
            throw new BaseException(ErrorCode.INTERNAL_ERROR, "Failed to create guest account");
        }
    }

    // ==========================================
    // 4. ROLE SELECTION
    // ==========================================
    @Transactional
    public AuthResponse selectRole(String userId, String roleName, String deviceId,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            if (userId == null || userId.isBlank()) {
                throw new BadRequestException("User ID is required for role selection");
            }

            if (roleName == null || roleName.isBlank()) {
                throw new BadRequestException("Role selection is required");
            }

            if (deviceId == null || deviceId.isBlank()) {
                throw new BadRequestException("Device ID is required for role selection");
            }

            log.info("Processing role selection for user {}: {}", userId, roleName);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

            if (user.getRole() != null) {
                throw new ConflictException(
                        "User already has a role assigned. Current role: " + user.getRole().getName()
                );
            }

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Role not found: " + roleName + ". Available roles: SHOPKEEPER, WHOLESALER"
                    ));

            if (!RoleConstants.SHOPKEEPER.equals(role.getName())
                    && !RoleConstants.WHOLESALER.equals(role.getName())) {
                throw new BadRequestException(
                        "Role '" + role.getName() + "' cannot be self-selected. Allowed roles: SHOPKEEPER, WHOLESALER"
                );
            }

            // 1. Save Role
            user.setRole(role);
            user.setAccountStatus(AccountStatus.PENDING_ONBOARDING);

            try {
                user = userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                log.error("Failed to save role for user: {}", e.getMessage());
                throw new BaseException(ErrorCode.INTERNAL_ERROR, "Failed to assign role", e.getMessage());
            }

            // 2. Delete ALL Refresh Tokens (Forces full authentication reset)
            refreshTokenService.revokeAll(user);

            // 3. Logout every device session
            if (user.getDevices() != null) {
                for (UserDevice device : user.getDevices()) {
                    try {
                        deviceSessionService.deactivateDevice(user, device.getDeviceId(), request);
                    } catch (Exception e) {
                        log.warn("Failed to deactivate device {}: {}", device.getDeviceId(), e.getMessage());
                    }
                }
            }

            log.info("Role {} selected for User {}. All previous sessions invalidated.", roleName, userId);

            auditService.log(AuditLogRequest.builder()
                    .userId(user.getId())
                    .action(AuditAction.ROLE_SELECTED)
                    .resourceType(ResourceType.USER)
                    .resourceId(role.getId())
                    .ipAddress(ipAddressService.getClientIp(request))
                    .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                    .deviceId(deviceId)
                    .status(AuditLog.Status.SUCCESS)
                    .newValue("Role assigned: " + roleName)
                    .build());

            // 4. Call Processor to generate NEW Device Session & NEW JWT Tokens
            return authFlowService.processLogin(user, user.getProvider().name(), deviceId, request, response);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Role selection failed for user {}: {}", userId, e.getMessage(), e);
            auditService.log(AuditLogRequest.builder()
                    .userId(userId != null ? userId : "UNKNOWN")
                    .action(AuditAction.ROLE_SELECTION_FAILED)
                    .resourceType(ResourceType.USER)
                    .ipAddress(ipAddressService.getClientIp(request))
                    .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                    .deviceId(deviceId)
                    .status(AuditLog.Status.FAILURE)
                    .failureReason(e.getMessage())
                    .build());
            throw new BaseException(ErrorCode.INTERNAL_ERROR, "Role selection failed: " + e.getMessage());
        }
    }

    // ==========================================
    // 5. REFRESH TOKEN FLOW - FIXED
    // ==========================================
    @Transactional
    public AuthResponse refresh(String refreshToken, String deviceId, HttpServletResponse response) {
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new BadRequestException("Refresh token is required");
            }

            if (deviceId == null || deviceId.isBlank()) {
                throw new BadRequestException("Device ID is required for token refresh");
            }

            log.debug("Processing token refresh for device: {}", deviceId);

            RefreshToken rotated = refreshTokenService.rotate(refreshToken, deviceId);
            User user = rotated.getUser();

            if (user.getAccountStatus() == AccountStatus.BLOCKED) {
                throw new BaseException(ErrorCode.ACCOUNT_BLOCKED,
                        "Account is blocked. Please contact support.");
            }

            if (user.getAccountStatus() == AccountStatus.DELETED) {
                throw new ResourceNotFoundException("Account no longer exists");
            }

            tokenService.generateAccessTokenOnly(user, deviceId, response);
            tokenService.setRefreshCookie(response, rotated.getToken());

            String roleName = user.getRole() != null ? user.getRole().getName() : null;

            return AuthResponse.builder()
                    .userId(user.getId())
                    .role(roleName)
                    .accountStatus(user.getAccountStatus())
                    .build();

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            throw new BaseException(ErrorCode.INVALID_TOKEN, "Token refresh failed: " + e.getMessage());
        }
    }

    // ==========================================
    // 6. LOGOUT FLOW
    // ==========================================
    @Transactional
    public void logout(String refreshToken, String deviceId, HttpServletRequest request, HttpServletResponse response) {
        String auditedUserId = "UNKNOWN";
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new BadRequestException("Refresh token is required for logout");
            }

            log.debug("Processing logout for device: {}", deviceId);

            RefreshToken token = refreshTokenService.revoke(refreshToken, deviceId);
            auditedUserId = token.getUser().getId();

            try {
                deviceSessionService.deactivateDevice(token.getUser(), deviceId, request);
            } catch (Exception e) {
                log.warn("Failed to deactivate device during logout: {}", e.getMessage());
            }

            tokenService.clear(response);

            auditService.log(AuditLogRequest.builder()
                    .userId(auditedUserId)
                    .action(AuditAction.LOGOUT)
                    .resourceType(ResourceType.AUTH)
                    .ipAddress(ipAddressService.getClientIp(request))
                    .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                    .deviceId(deviceId)
                    .status(AuditLog.Status.SUCCESS)
                    .build());

            log.info("User {} logged out from device {}", auditedUserId, deviceId);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Logout failed for device {}: {}", deviceId, e.getMessage(), e);
            auditService.log(AuditLogRequest.builder()
                    .userId(auditedUserId)
                    .action(AuditAction.LOGOUT)
                    .resourceType(ResourceType.AUTH)
                    .ipAddress(ipAddressService.getClientIp(request))
                    .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                    .deviceId(deviceId)
                    .status(AuditLog.Status.FAILURE)
                    .failureReason("Logout failed: " + e.getMessage())
                    .build());
            throw new BaseException(ErrorCode.INTERNAL_ERROR, "Logout failed: " + e.getMessage());
        }
    }

    // ==========================================
    // 7. LOGOUT ALL FLOW
    // ==========================================
    @Transactional
    public void logoutAll(String userId, String deviceId, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (userId == null || userId.isBlank()) {
                throw new BadRequestException("User ID is required for global logout");
            }

            log.debug("Processing global logout for user: {}", userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

            refreshTokenService.revokeAll(user);
            tokenService.clear(response);

            if (user.getDevices() != null) {
                for (UserDevice device : user.getDevices()) {
                    try {
                        deviceSessionService.deactivateDevice(user, device.getDeviceId(), request);
                    } catch (Exception e) {
                        log.warn("Failed to deactivate device {} during global logout: {}", device.getDeviceId(), e.getMessage());
                    }
                }
            }

            auditService.log(AuditLogRequest.builder()
                    .userId(user.getId())
                    .action(AuditAction.LOGOUT_ALL)
                    .resourceType(ResourceType.USER)
                    .ipAddress(ipAddressService.getClientIp(request))
                    .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                    .deviceId(deviceId)
                    .status(AuditLog.Status.SUCCESS)
                    .build());

            log.info("User {} executed global logout.", userId);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Global logout failed for user {}: {}", userId, e.getMessage(), e);
            throw new BaseException(ErrorCode.INTERNAL_ERROR, "Global logout failed: " + e.getMessage());
        }
    }
}