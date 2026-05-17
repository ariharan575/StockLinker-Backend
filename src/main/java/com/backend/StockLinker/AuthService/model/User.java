package com.backend.StockLinker.AuthService.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Setter
@Getter
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("account_status != 'BLOCKED'")
public class User extends BaseEntity {

    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "full_name", length = 100)
    private String name;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "unique_id", unique = true, length = 100)
    private String uniqueId;

    @Column(name = "provider", length = 50)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "account_locked")
    @Builder.Default
    private boolean accountLocked = false;

    @Column(name = "failed_attempt")
    @Builder.Default
    private int failedAttempts = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 25)
    private String lastLoginIp;

    @Column(name = "last_login_user_agent", length = 500)
    private String lastLoginUserAgent;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"})
    )
    @Builder.Default  // ✅ Important: Use @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserDevice> devices = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AuditLog> auditLogs = new ArrayList<>();

    // =========================================================
    // 🔐 ACCOUNT MANAGEMENT METHODS
    // =========================================================

    public void incrementFailedAttempts() {
        this.failedAttempts++;
        if (this.failedAttempts >= 5) {
            this.accountLocked = true;
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.accountLocked = false;
    }

    public boolean isActive() {
        return this.accountStatus == AccountStatus.ACTIVE && !this.accountLocked;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }

    public boolean hasAnyRole(String... roleNames) {
        Set<String> roleNameSet = Set.of(roleNames);
        return roles.stream().anyMatch(role -> roleNameSet.contains(role.getName()));
    }

    public boolean hasPermission(String permissionName) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    public enum AccountStatus {
        ACTIVE, BLOCKED, PENDING
    }

    public User(String phone) {
        this.phone = phone;
    }
}