package com.backend.StockLinker.ProfileService.dto.response;

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
public class CompareDataResponse {

    private HeaderMetrics headerMetrics;
    private List<AiVolumeDeal> aiVolumeDeals;
    private List<SupplierRow> suppliers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeaderMetrics {
        private String productName;
        private int supplierCount;
        private BigDecimal bestPrice;
        private BigDecimal averagePrice;
        private BigDecimal savingsPerUnit;
        private BigDecimal totalSavings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiVolumeDeal {
        private int rank;
        private String sellerId;
        private String businessName;
        private String location;
        private Double rating;
        private Integer reviewCount;
        private int requiredQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalSavingsVsMarket;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierRow {
        private String id;
        private String sellerId;
        private String businessName;
        private String initials;
        private boolean verified;
        private String location;
        private Double rating;
        private Integer reviews;
        private int moq;
        private String moqUnit;
        private BigDecimal moqPrice;
        private BigDecimal calculatedUnitPrice;
        private Integer bulkQty;
        private BigDecimal bulkPrice;
        private int availableStock;
        private Integer trustScore;
        private String deliveryTime;
        private String badge;
    }
}