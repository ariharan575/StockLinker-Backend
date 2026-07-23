package com.backend.StockLinker.ProfileService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontResponse {
    private String businessId;
    private String businessName;
    private String ownerName;
    private String businessType;
    private String location;

    // --- New Fields for Profile & Delivery Tabs ---
    private String fullAddress;
    private String storeSize;
    private Integer coverageRadiusKm;
    private BigDecimal minimumOrderValue;
    private BigDecimal deliveryCharge;
    private String operatingDays;
    // ---------------------------------------------

    private Double rating;
    private Integer reviewCount;
    private Integer yearsInBusiness;
    private String businessEmail;
    private String mobileNumber;
    private String gstNumber;
    private boolean deliverySupported;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private List<StorefrontProductDto> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorefrontProductDto {
        private String id;
        private String productName;
        private String brand;
        private String category;
        private String unit;
        private String packageSize;
        private BigDecimal price;
        private Integer minimumOrderQuantity;
        private Integer bulkDealQuantity;
        private BigDecimal bulkDealPrice;
        private Integer availableStock;
    }
}