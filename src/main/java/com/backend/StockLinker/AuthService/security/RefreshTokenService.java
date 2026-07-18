package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.enums.AuditAction;
import com.backend.StockLinker.AuthService.enums.ResourceType;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.exception.customExceptions.InvalidTokenException;
import com.backend.StockLinker.AuthService.exception.customExceptions.TokenExpiredException;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.RefreshToken;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.model.UserDevice;
import com.backend.StockLinker.AuthService.repository.RefreshTokenRepository;
import com.backend.StockLinker.AuthService.repository.UserDeviceRepository;
import com.backend.StockLinker.AuthService.service.AuditService;
import com.backend.StockLinker.AuthService.service.IpAddressService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final UserDeviceRepository userDeviceRepository;
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenDuration;

    // =========================================================
    // ✅ CREATE TOKEN (LOGIN & ROTATION)
    // =========================================================
    @Transactional
    public RefreshToken create(User user, String deviceId) {

        if (user == null || user.getId() == null) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "User is required for token creation");
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Device ID is required for token creation");
        }

        String tokenId = UUID.randomUUID().toString();
        String rawToken = UUID.randomUUID().toString();

        // Generate BCrypt hash of the raw token
        String hashedToken = BCrypt.hashpw(rawToken, BCrypt.gensalt());

        HttpServletRequest request = getHttpRequest();
        String ipAddress = (request != null) ? ipAddressService.getClientIp(request) : null;
        String userAgent = (request != null) ? request.getHeader("User-Agent") : null;

        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .orElseThrow(() -> {
                    log.error("Device not found for user {} and deviceId {}", user.getId(), deviceId);
                    return new InvalidTokenException("Device not registered. Please login again.");
                });

        log.debug("Device found for token creation: {} for user {}", device.getDeviceId(), user.getId());

        // Store ONLY the BCrypt hash in the 'token' field
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .device(device)
                .token(hashedToken)
                .tokenId(tokenId)
                .deviceId(deviceId)
                .expiryDate(Instant.now().plusMillis(refreshTokenDuration))
                .revoked(false)
                .createdIp(ipAddress)
                .createdUserAgent(userAgent)
                .lastUsedAt(Instant.now())
                .build();

        RefreshToken savedToken = repository.save(token);

        // ✅ THE FIX: Stop JPA Dirty Checking from overwriting the hash
        entityManager.flush(); // Force the hash to be written to the DB immediately
        entityManager.detach(savedToken); // Detach entity so changes aren't tracked by Hibernate

        // Now it is safe to set the plaintext token on the unmanaged object for the response
        String combinedTokenForResponse = tokenId + "." + rawToken;
        savedToken.setToken(combinedTokenForResponse);

        auditService.log(AuditLogRequest.builder()
                .userId(user.getId())
                .action(AuditAction.REFRESH_TOKEN_CREATED)
                .resourceType(ResourceType.TOKEN)
                .resourceId(tokenId)
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(AuditLog.Status.SUCCESS)
                .build());

        log.debug("Refresh token created successfully for user {} and device {}", user.getId(), deviceId);

        return savedToken;
    }

    // =========================================================
    // 🔍 VALIDATE TOKEN
    // =========================================================
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public RefreshToken validate(String fullToken, String deviceId) {

        if (fullToken == null || fullToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is missing");
        }

        String[] parts = fullToken.split("\\.");

        if (parts.length != 2) {
            throw new InvalidTokenException("Invalid token format. Expected: tokenId.rawSecret");
        }

        String tokenId = parts[0];
        String rawToken = parts[1];

        try {
            UUID.fromString(tokenId);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid tokenId format");
        }

        RefreshToken stored = repository.findByTokenId(tokenId)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        System.out.println(fullToken + " device id" + deviceId);

        String storedHash = stored.getToken();

        if (storedHash == null || storedHash.isBlank()) {
            log.error("Stored token hash is null or empty for tokenId: {}", tokenId);
            throw new InvalidTokenException("Invalid token data");
        }

        // Verify the raw token against the stored hash using BCrypt
        if (!BCrypt.checkpw(rawToken, storedHash)) {
            log.warn("Token validation failed for tokenId: {}", tokenId);
            throw new InvalidTokenException("Token validation failed - invalid signature");
        }

        User tokenUser = stored.getUser();
        Hibernate.initialize(tokenUser);

        // 🚨 TOKEN REPLAY DETECTION
        if (stored.isRevoked()) {
            if (stored.getReplacedByToken() != null) {
                log.error("CRITICAL: Token theft (Replay Attack) detected for user {}", tokenUser.getId());

                auditService.log(AuditLogRequest.builder()
                        .userId(tokenUser.getId())
                        .action(AuditAction.REPLAY_ATTACK)
                        .resourceType(ResourceType.TOKEN)
                        .resourceId(tokenId)
                        .deviceId(deviceId)
                        .status(AuditLog.Status.FAILURE)
                        .failureReason("Reused a revoked refresh token")
                        .build());

                revokeAll(tokenUser);
                throw new InvalidTokenException("Security violation. All sessions terminated.");
            }
            throw new InvalidTokenException("Token has been revoked");
        }

        if (stored.isExpired()) {
            stored.revoke();
            repository.save(stored);
            throw new TokenExpiredException("Refresh token has expired");
        }

        if (stored.getDeviceId() == null || !stored.getDeviceId().equals(deviceId)) {
            System.out.println("Stored = " + stored.getDeviceId());
            System.out.println("Request = " + deviceId);
            throw new InvalidTokenException("Device mismatch for refresh token");
        }

        // Update Usage Analytics
        stored.setLastUsedAt(Instant.now());
        repository.save(stored);

        return stored;
    }

    // =========================================================
    // 🔄 ROTATE TOKEN
    // =========================================================
    @Transactional
    public RefreshToken rotate(String oldToken, String deviceId) {

        RefreshToken existing = validate(oldToken, deviceId);

        // Create new token FIRST
        RefreshToken newToken = create(existing.getUser(), deviceId);

        // Link the family chain securely
        existing.markAsReplaced(newToken.getTokenId());

        repository.save(existing);

        // Update Device Timeline
        if (existing.getDevice() != null) {
            existing.getDevice().setLastRefreshAt(LocalDateTime.now());
            existing.getDevice().setLastActivityAt(LocalDateTime.now());
            userDeviceRepository.save(existing.getDevice());
        }

        auditService.log(AuditLogRequest.builder()
                .userId(existing.getUser().getId())
                .action(AuditAction.TOKEN_ROTATED)
                .resourceType(ResourceType.TOKEN)
                .resourceId(newToken.getTokenId())
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .build());

        return newToken;
    }

    // =========================================================
    // 🚪 REVOKE SINGLE DEVICE TOKEN
    // =========================================================
    @Transactional
    public RefreshToken revoke(String token, String deviceId) {

        RefreshToken existing = validate(token, deviceId);

        if (existing.getDevice() != null) {
            existing.getDevice().setLogoutAt(LocalDateTime.now());
            userDeviceRepository.save(existing.getDevice());
        }

        existing.revoke();
        repository.save(existing);

        auditService.log(AuditLogRequest.builder()
                .userId(existing.getUser().getId())
                .action(AuditAction.TOKEN_REVOKED)
                .resourceType(ResourceType.TOKEN)
                .resourceId(existing.getTokenId())
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .build());

        return existing;
    }

    // =========================================================
    // 🚪 REVOKE ALL USER TOKENS
    // =========================================================
    @Transactional
    public void revokeAll(User user) {

        repository.revokeAllTokensForDevice(user.getId());

        auditService.log(AuditLogRequest.builder()
                .userId(user.getId())
                .action(AuditAction.LOGOUT_ALL)
                .resourceType(ResourceType.USER)
                .resourceId(user.getId())
                .status(AuditLog.Status.SUCCESS)
                .build());
    }

    // =========================================================
    // 🌐 UTILITY
    // =========================================================
    private HttpServletRequest getHttpRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attrs != null) ? attrs.getRequest() : null;
    }
}