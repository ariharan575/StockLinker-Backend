package com.backend.StockLinker.ProfileService.dto.request;

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
public class SellerProductRequest {

    @NotBlank(message = "Master Product ID is required")
    private String masterProductId;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Package size is required")
    private String packageSize;

    @NotBlank(message = "Unit is required")
    private String unit;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Minimum order quantity is required")
    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    private Integer minimumOrderQuantity;

    private Integer bulkDealQuantity;

    private BigDecimal bulkDealPrice;

    @NotNull(message = "Available stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer availableStock;
}