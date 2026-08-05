package com.backend.StockLinker.Global_Request_Service.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalEnquiryRequest {
    @NotBlank(message = "Master Product ID is required")
    private String masterProductId;

    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer requestedQuantity;

    @NotNull(message = "Target price is required")
    @Min(value = 0, message = "Target price cannot be negative")
    private BigDecimal targetPrice;

    private String message;
}