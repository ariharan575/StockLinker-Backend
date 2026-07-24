package com.backend.StockLinker.Onboading_Service.dto.request;

import com.backend.StockLinker.Onboading_Service.enums.StoreSize;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Set;

@Data
public class MarketplaceInfoRequestDto {
    @NotEmpty(message = "At least one business category must be selected")
    private Set<String> categoryIds;

    private Boolean deliveryAvailable;
    private StoreSize storeSize;
}