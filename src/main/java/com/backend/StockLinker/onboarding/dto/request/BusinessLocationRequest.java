package com.backend.StockLinker.onboarding.dto.request;

import com.backend.StockLinker.onboarding.validation.annotation.ValidPincode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for business location onboarding step.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessLocationRequest {

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String district;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    @ValidPincode
    private String pincode;

    @Size(max = 255, message = "Landmark cannot exceed 255 characters")
    private String landmark;

    @DecimalMin(value = "-90.0000000", message = "Latitude must be greater than or equal to -90")
    @DecimalMax(value = "90.0000000", message = "Latitude must be less than or equal to 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0000000", message = "Longitude must be greater than or equal to -180")
    @DecimalMax(value = "180.0000000", message = "Longitude must be less than or equal to 180")
    private BigDecimal longitude;
}
