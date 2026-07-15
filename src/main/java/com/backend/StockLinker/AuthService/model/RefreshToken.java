package com.backend.StockLinker.AuthService.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_token_id", columnList = "token_id", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_device_id", columnList = "device_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"user", "device"})
@EqualsAndHashCode(callSuper = true, exclude = {"user", "device"})
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String token; // Stores only the BCrypt Hash of the secret

    @Column(name = "token_id", unique = true, nullable = false, length = 100)
    private String tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_fk", columnDefinition = "VARCHAR(36)")
    private UserDevice device;

    @Column(name = "device_id", length = 100, nullable = false)
    private String deviceId;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "replaced_by_token", length = 100)
    private String replacedByToken; // Points strictly to the new tokenId

    // --- Enterprise Metadata ---
    @Column(name = "created_ip", length = 45)
    private String createdIp;

    @Column(name = "created_user_agent", length = 255)
    private String createdUserAgent;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    // =========================================================
    // 🔍 STATE CHECKS
    // =========================================================
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    // =========================================================
    // 🔄 REVOKE THIS TOKEN
    // =========================================================
    public void revoke() {
        this.revoked = true;
    }

    // =========================================================
    // 🔁 MARK AS REPLACED BY NEW TOKEN
    // =========================================================
    public void markAsReplaced(String newTokenId) {
        this.revoked = true;
        this.replacedByToken = newTokenId;
        this.rotatedAt = Instant.now();
    }
}