package com.backend.StockLinker.AuthService.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import com.backend.StockLinker.AuthService.enums.AccountStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 25)
    private String lastLoginIp;

    @Column(name = "last_login_user_agent", length = 500)
    private String lastLoginUserAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"})
    )
    private String role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserDevice> devices = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AuditLog> auditLogs = new ArrayList<>();
}