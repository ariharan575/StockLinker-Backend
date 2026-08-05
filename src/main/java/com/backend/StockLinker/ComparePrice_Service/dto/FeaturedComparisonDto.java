package com.backend.StockLinker.ComparePrice_Service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedComparisonDto {
    private String masterProductId;
    private String productName;
    private String brand;
    private List<FeaturedSupplierDto> suppliers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturedSupplierDto {
        private String name;
        private BigDecimal price;
        private boolean isBest;
    }
}