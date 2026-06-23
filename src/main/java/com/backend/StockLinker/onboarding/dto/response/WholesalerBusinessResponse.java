package com.backend.StockLinker.onboarding.dto.response;

import com.backend.StockLinker.onboarding.enums.DeliverySupportType;
import com.backend.StockLinker.onboarding.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for wholesaler business details.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WholesalerBusinessResponse {

    private Long id;

    private Long userId;

    private Set<ProductCategory> productCategories;

    private DeliverySupportType deliverySupport;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
