package com.backend.StockLinker.Auth_Service.service;

import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Auth_Service.dto.response.AuthResponse;
import com.backend.StockLinker.Auth_Service.enums.*;
import com.backend.StockLinker.Exception.BaseException;
import com.backend.StockLinker.Exception.ErrorCode;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Auth_Service.model.RefreshToken;
import com.backend.StockLinker.Auth_Service.model.Role;
import com.backend.StockLinker.Auth_Service.model.User;
import com.backend.StockLinker.Auth_Service.model.UserDevice;
import com.backend.StockLinker.Auth_Service.repository.RoleRepository;
import com.backend.StockLinker.Auth_Service.repository.UserRepository;
import com.backend.StockLinker.Auth_Service.repository.UserDeviceRepository;
import com.backend.StockLinker.Security.RefreshTokenService;
import com.backend.StockLinker.Security.TokenService;
import com.backend.StockLinker.Exception.customExceptions.*;
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

            return authFlowService.processLogin(user, Provider.GOOGLE.name(), deviceId, request, response);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage(), e);
            throw new BaseException(ErrorCode.OAUTH_FAILED, "Google authentication failed: " + e.getMessage());
        }
    }

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

    @Transactional
    public AuthResponse guestLogin(String deviceId, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (deviceId == null || deviceId.isBlank()) {
                throw new BadRequestException("Device ID is required for guest login");
            }

            log.info("Processing guest login for device: {}", deviceId);

            // 1. Check if a Guest account already exists for this exact device ID
            User existingGuest = userRepository.findGuestUserByDeviceId(deviceId).orElse(null);

            if (existingGuest != null) {
                log.info("Existing guest found: {}. Logging them back in.", existingGuest.getId());
                return authFlowService.processLogin(existingGuest, Provider.GUEST.name(), deviceId, request, response);
            }

            // 2. If no Guest account exists for this device, create a brand new one
            log.info("No existing guest found for device {}. Creating new guest account.", deviceId);

            User guest = new User();
            guest.setName("Guest-" + UUID.randomUUID().toString().substring(0, 8));
            guest.setUniqueId(UUID.randomUUID().toString());
            guest.setProvider(Provider.GUEST);
            guest.setRole(null);
            guest.setAccountStatus(AccountStatus.PENDING_ROLE);

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

            user.setRole(role);
            user.setAccountStatus(AccountStatus.PENDING_ONBOARDING);

            try {
                user = userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                log.error("Failed to save role for user: {}", e.getMessage());
                throw new BaseException(ErrorCode.INTERNAL_ERROR, "Failed to assign role", e.getMessage());
            }

            refreshTokenService.revokeAll(user);

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