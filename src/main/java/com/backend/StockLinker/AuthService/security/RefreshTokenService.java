package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
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

        String tokenId = UUID.randomUUID().toString(); // 🔥 PUBLIC ID
        String rawToken = UUID.randomUUID().toString(); // 🔐 SECRET
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

        // 🔥 RETURN COMBINED TOKEN (tokenId.rawToken)
        token.setToken(tokenId + "." + rawToken);

        return token;
    }

    // =========================================================
    // 🔍 VALIDATE TOKEN (SECURE)
    // =========================================================
    @Transactional(readOnly = true)
    public RefreshToken validate(String fullToken, String deviceId) {

        String[] parts = fullToken.split("\\.");

        if (parts.length != 2) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }

        String tokenId = parts[0];
        String rawToken = parts[1];

        RefreshToken stored = repository.findByTokenId(tokenId)
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_TOKEN));

        // 🔐 CHECK HASH
        if (!BCrypt.checkpw(rawToken, stored.getToken())) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }

        // 🔒 REVOKED
        if (stored.isRevoked()) {
            throw new BaseException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        // ⏳ EXPIRED
        if (stored.isExpired()) {
            throw new BaseException(ErrorCode.TOKEN_EXPIRED);
        }

        // 📱 DEVICE CHECK (STRICT)
        if (!stored.getDeviceId().equals(deviceId)) {
            throw new BaseException(ErrorCode.FORBIDDEN, "Device mismatch");
        }

        return stored;
    }

    // =========================================================
    // 🔄 ROTATE TOKEN (CRITICAL SECURITY)
    // =========================================================
    @Transactional
    public RefreshToken rotate(String oldToken, String deviceId) {

        RefreshToken existing = validate(oldToken, deviceId);

        // 🔥 REVOKE OLD
        existing.setRevoked(true);

        // 🔁 CREATE NEW
        RefreshToken newToken = create(existing.getUser(), deviceId);

        // 🔗 LINK CHAIN (OPTIONAL BUT GOOD)
        existing.setReplacedByToken(newToken.getToken());

        repository.save(existing);

        log.info("Token rotated for user: {}", existing.getUser().getId());

        return newToken;
    }

    // =========================================================
    // 🚪 LOGOUT SINGLE DEVICE
    // =========================================================
    @Transactional
    public void revoke(String token, String deviceId) {

        RefreshToken existing = validate(token, deviceId);

        existing.setRevoked(true);

        repository.save(existing);
    }

    // =========================================================
    // 🚪 LOGOUT ALL DEVICES
    // =========================================================
    @Transactional
    public void revokeAll(User user) {

        repository.revokeAllUserTokens(user.getId());

        log.info("All tokens revoked for user: {}", user.getId());
    }
}