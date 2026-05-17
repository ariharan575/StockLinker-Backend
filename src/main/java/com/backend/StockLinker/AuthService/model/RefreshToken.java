package com.backend.StockLinker.AuthService.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_token_id", columnList = "token_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_device_id", columnList = "device_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "token_id", unique = true, nullable = false, length = 100)
    private String tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_fk")
    private UserDevice device;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "replaced_by_token", length = 255)
    private String replacedByToken;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    // =========================================================
    // 🔍 CHECK IF TOKEN IS EXPIRED
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
    public void markAsReplaced(String newToken) {
        this.revoked = true;
        this.replacedByToken = newToken;
    }
}