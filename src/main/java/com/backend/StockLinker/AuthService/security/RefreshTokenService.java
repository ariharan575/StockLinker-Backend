package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.exception.customExceptions.InvalidTokenException;
import com.backend.StockLinker.AuthService.exception.customExceptions.TokenExpiredException;
import com.backend.StockLinker.AuthService.model.RefreshToken;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenDuration;

    // =========================================================
    // ✅ CREATE TOKEN (LOGIN)
    // =========================================================
    @Transactional
    public RefreshToken create(User user, String deviceId) {
        String tokenId = UUID.randomUUID().toString();
        String rawToken = UUID.randomUUID().toString();
        String hashed = BCrypt.hashpw(rawToken, BCrypt.gensalt());

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(hashed)
                .tokenId(tokenId)
                .deviceId(deviceId)
                .expiryDate(Instant.now().plusMillis(refreshTokenDuration))
                .revoked(false)
                .build();

        repository.save(token);
        log.info("Refresh token created for user: {}", user.getId());

        // Return combined token
        token.setToken(tokenId + "." + rawToken);
        return token;
    }

    // =========================================================
    // 🔍 VALIDATE TOKEN (USING CUSTOM EXCEPTIONS)
    // =========================================================
    @Transactional(readOnly = true)
    public RefreshToken validate(String fullToken, String deviceId) {
        String[] parts = fullToken.split("\\.");

        if (parts.length != 2) {
            throw new InvalidTokenException("Invalid token format");
        }

        String tokenId = parts[0];
        String rawToken = parts[1];

        RefreshToken stored = repository.findByTokenId(tokenId)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        // Check hash
        if (!BCrypt.checkpw(rawToken, stored.getToken())) {
            throw new InvalidTokenException("Token validation failed");
        }

        // Check revoked
        if (stored.isRevoked()) {
            throw new InvalidTokenException("Token has been revoked");
        }

        // Check expired
        if (stored.isExpired()) {
            throw new TokenExpiredException("Refresh token has expired");
        }

        // Device check
        if (!stored.getDeviceId().equals(deviceId)) {
            throw new InvalidTokenException("Device mismatch");
        }

        return stored;
    }

    // =========================================================
    // 🔄 ROTATE TOKEN (CRITICAL SECURITY)
    // =========================================================
    @Transactional
    public RefreshToken rotate(String oldToken, String deviceId) {
        RefreshToken existing = validate(oldToken, deviceId);

        // Revoke old token
        existing.setRevoked(true);

        // Create new token
        RefreshToken newToken = create(existing.getUser(), deviceId);

        // Link the chain
        existing.setReplacedByToken(newToken.getToken());
        repository.save(existing);

        log.info("Token rotated for user: {}", existing.getUser().getId());
        return newToken;
    }

    // =========================================================
    // 🚪 REVOKE SINGLE DEVICE TOKEN
    // =========================================================
    @Transactional
    public void revoke(String token, String deviceId) {
        RefreshToken existing = validate(token, deviceId);
        existing.setRevoked(true);
        repository.save(existing);
        log.info("Token revoked for user: {}", existing.getUser().getId());
    }

    // =========================================================
    // 🚪 REVOKE ALL USER TOKENS
    // =========================================================
    @Transactional
    public void revokeAll(User user) {
        repository.revokeAllUserTokens(user.getId());
        log.info("All tokens revoked for user: {}", user.getId());
    }
}