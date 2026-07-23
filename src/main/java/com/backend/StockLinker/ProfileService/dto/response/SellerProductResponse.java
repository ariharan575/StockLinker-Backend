package com.backend.StockLinker.ProfileService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProductResponse {
    private String id;
    private String productName;
    private String brand;
    private String category;
    private String unit;
    private String packageSize;
    private BigDecimal price;
    private Integer minimumOrderQuantity;
    private Integer availableStock;
    private String status;
    private LocalDateTime updatedAt;
}