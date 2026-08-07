package com.backend.StockLinker.Seller_Inventary_Service.Dto;

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
    private BigDecimal price;
    private Integer minimumOrderQuantity;
    private Integer bulkDealQuantity;
    private BigDecimal bulkDealPrice;
    private Integer availableStock;
    private String status;
    private LocalDateTime updatedAt;
}