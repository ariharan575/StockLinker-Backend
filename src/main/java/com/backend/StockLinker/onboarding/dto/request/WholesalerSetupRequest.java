package com.backend.StockLinker.onboarding.dto.request;

import com.backend.StockLinker.onboarding.enums.DeliverySupportType;
import com.backend.StockLinker.onboarding.enums.ProductCategory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Request DTO for wholesaler setup onboarding step.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WholesalerSetupRequest {

    @NotEmpty(message = "At least one product category is required")
    private Set<ProductCategory> productCategories;

    @NotNull(message = "Delivery support is required")
    private DeliverySupportType deliverySupport;
}