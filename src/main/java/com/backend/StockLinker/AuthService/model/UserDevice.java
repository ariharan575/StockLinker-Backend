package com.backend.StockLinker.AuthService.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_devices")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class UserDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false, unique = true)
    private String deviceId;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "device_type", length = 100)
    private String deviceType;

    @Column(length = 100)
    private String os;

    @Column(name = "os_version", length = 100)
    private String osVersion;

    @Column(length = 100)
    private String browser;

    @Column(name = "browser_version", length = 100)
    private String browserVersion;

    @Column(length = 100)
    private String platform;

    @Column(length = 100)
    private String manufacturer;

    @Column(length = 100)
    private String model;

    @Column(name = "app_version", length = 100)
    private String appVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String timezone;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> tokens;

    @Column(name = "login_at")
    private LocalDateTime loginAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    @Column(name = "last_refresh_at")
    private LocalDateTime lastRefreshAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "is_trusted", nullable = false)
    @Builder.Default
    private boolean trusted = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Transient
    @Builder.Default
    private boolean currentDevice = false;
}