package com.backend.StockLinker.onboarding.dto.response;

import com.backend.StockLinker.onboarding.enums.PurchaseCategory;
import com.backend.StockLinker.onboarding.enums.StoreType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for shopkeeper business details.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopkeeperBusinessResponse {

    private Long id;

    private Long userId;

    private Set<PurchaseCategory> purchaseCategories;

    private StoreType storeType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}