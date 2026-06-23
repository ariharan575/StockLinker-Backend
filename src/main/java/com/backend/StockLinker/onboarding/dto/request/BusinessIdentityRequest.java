package com.backend.StockLinker.onboarding.dto.request;

import com.backend.StockLinker.onboarding.enums.BusinessType;
import com.backend.StockLinker.onboarding.validation.annotation.ValidBusinessName;
import com.backend.StockLinker.onboarding.validation.annotation.ValidMobileNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for business identity onboarding step.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessIdentityRequest {

    @NotBlank(message = "Owner name is required")
    @Size(max = 120, message = "Owner name cannot exceed 120 characters")
    private String ownerName;

    @ValidBusinessName
    private String businessName;

    @NotNull(message = "Business type is required")
    private BusinessType businessType;

    @NotBlank(message = "Business category is required")
    @Size(max = 100, message = "Business category cannot exceed 100 characters")
    private String businessCategory;

    @ValidMobileNumber
    private String mobile;

    @ValidMobileNumber
    private String alternateMobile;

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;
}