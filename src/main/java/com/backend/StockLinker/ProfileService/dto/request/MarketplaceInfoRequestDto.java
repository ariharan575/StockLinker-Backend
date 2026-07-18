package com.backend.StockLinker.ProfileService.dto.request;

import com.backend.StockLinker.ProfileService.enums.StoreSize;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class MarketplaceInfoRequestDto {

    @NotEmpty(message = "At least one business category must be selected")
    private Set<String> categoryIds; // Getting IDs from Frontend

    private Boolean deliveryAvailable;

    private StoreSize storeSize;
}