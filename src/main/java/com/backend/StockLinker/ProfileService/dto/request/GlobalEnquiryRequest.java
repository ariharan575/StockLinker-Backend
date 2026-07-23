package com.backend.StockLinker.ProfileService.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class GlobalEnquiryRequest {
    @NotBlank(message = "Master Product ID is required")
    private String masterProductId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer requestedQuantity;

    private BigDecimal targetPrice;

    private String message;
}