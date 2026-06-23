package com.backend.StockLinker.onboarding.dto.request;

import com.backend.StockLinker.onboarding.enums.PurchaseCategory;
import com.backend.StockLinker.onboarding.enums.StoreType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Request DTO for shopkeeper setup onboarding step.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopkeeperSetupRequest {

    @NotEmpty(message = "At least one purchase category is required")
    private Set<PurchaseCategory> purchaseCategories;

    @NotNull(message = "Store type is required")
    private StoreType storeType;
}
