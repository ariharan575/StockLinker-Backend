package com.backend.StockLinker.onboarding.entity;

import com.backend.StockLinker.onboarding.enums.AccountStatus;
import com.backend.StockLinker.onboarding.enums.BusinessType;
import com.backend.StockLinker.onboarding.enums.OnboardingStep;
import com.backend.StockLinker.onboarding.enums.PreferredLanguage;
import com.backend.StockLinker.onboarding.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores common onboarding business profile details.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "common_business_profiles",
        indexes = {
                @Index(name = "idx_common_profile_user_id", columnList = "user_id"),
                @Index(name = "idx_common_profile_mobile", columnList = "mobile"),
                @Index(name = "idx_common_profile_email", columnList = "email"),
                @Index(name = "idx_common_profile_city", columnList = "city"),
                @Index(name = "idx_common_profile_state", columnList = "state"),
                @Index(name = "idx_common_profile_completion", columnList = "completion_percentage")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_common_profile_user_id", columnNames = "user_id")
        }
)
public class CommonBusinessProfile extends BaseAuditEntity {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", length = 20)
    private PreferredLanguage preferredLanguage;

    @Column(name = "owner_name", length = 120)
    private String ownerName;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 50)
    private BusinessType businessType;

    @Column(name = "business_category", length = 100)
    private String businessCategory;

    @Column(name = "mobile", length = 15)
    private String mobile;

    @Column(name = "alternate_mobile", length = 15)
    private String alternateMobile;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "landmark", length = 255)
    private String landmark;

    @Column(name = "profile_photo", length = 500)
    private String profilePhoto;

    @Column(name = "business_logo", length = 500)
    private String businessLogo;

    @Column(name = "business_banner", length = 500)
    private String businessBanner;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_step", length = 50)
    private OnboardingStep onboardingStep;

    @Column(name = "completion_percentage")
    private Integer completionPercentage;

    @Column(name = "is_draft_saved", nullable = false)
    @Builder.Default
    private Boolean isDraftSaved = Boolean.FALSE;

    @Column(name = "is_onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean isOnboardingCompleted = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 50)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", length = 50)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING_APPROVAL;

    @Column(name = "last_profile_updated_at")
    private LocalDateTime lastProfileUpdatedAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
}