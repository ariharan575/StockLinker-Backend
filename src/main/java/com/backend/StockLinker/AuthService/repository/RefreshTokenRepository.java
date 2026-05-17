package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    // =========================================================
    // 🔍 FIND BY TOKEN ID
    // =========================================================
    Optional<RefreshToken> findByTokenId(String tokenId);

    // =========================================================
    // 🔍 FIND BY TOKEN
    // =========================================================
    Optional<RefreshToken> findByToken(String token);

    // =========================================================
    // 🔍 FIND ACTIVE TOKEN BY USER AND DEVICE
    // =========================================================
    Optional<RefreshToken> findByUserIdAndDeviceIdAndRevokedFalse(String userId, String deviceId);

    // =========================================================
    // 🔄 REVOKE ALL USER TOKENS
    // =========================================================
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllUserTokens(@Param("userId") String userId);

    // =========================================================
    // 🔄 REVOKE SPECIFIC DEVICE TOKENS
    // =========================================================
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.deviceId = :deviceId AND rt.revoked = false")
    void revokeDeviceTokens(@Param("deviceId") String deviceId);

    // =========================================================
    // 🗑️ DELETE EXPIRED TOKENS (CLEANUP)
    // =========================================================
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
    int deleteExpiredTokens();
}