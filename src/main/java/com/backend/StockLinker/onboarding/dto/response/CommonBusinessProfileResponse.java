package com.backend.StockLinker.onboarding.dto.response;

import com.backend.StockLinker.onboarding.enums.AccountStatus;
import com.backend.StockLinker.onboarding.enums.BusinessType;
import com.backend.StockLinker.onboarding.enums.OnboardingStep;
import com.backend.StockLinker.onboarding.enums.PreferredLanguage;
import com.backend.StockLinker.onboarding.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for common business profile.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonBusinessProfileResponse {

    private Long id;

    private Long userId;

    private PreferredLanguage preferredLanguage;

    private String ownerName;

    private String businessName;

    private BusinessType businessType;

    private String businessCategory;

    private String mobile;

    private String alternateMobile;

    private String email;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String district;

    private String state;

    private String country;

    private String pincode;

    private String landmark;

    private String profilePhoto;

    private String businessLogo;

    private String businessBanner;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private OnboardingStep onboardingStep;

    private Integer completionPercentage;

    private Boolean isDraftSaved;

    private Boolean isOnboardingCompleted;

    private VerificationStatus verificationStatus;

    private AccountStatus accountStatus;

    private LocalDateTime lastProfileUpdatedAt;

    private LocalDateTime lastActiveAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}