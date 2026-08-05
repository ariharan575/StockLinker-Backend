package com.backend.StockLinker.SellerProfile_Service.dto;

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
public class SellerProfileResponse {
    private String businessId;
    private String userId;
    private String businessName;
    private String ownerName;
    private String businessType;

    // --- Enhanced Profile Data (SaaS Level) ---
    private String businessDescription;
    private String website;
    private String whatsappNumber;
    private String verificationStatus;
    private Integer trustScore;
    private Integer marketplaceRank;
    private Integer totalOrdersFulfilled;
    private boolean hasRated; // NEW: To lock the rating UI if already rated

    private List<String> primaryCategoryNames;

    // --- Granular Structured Address ---
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String state;
    private String pincode;
    private String landmark;

    // --- Logistics & Operations ---
    private String storeSize;
    private Integer coverageRadiusKm;
    private BigDecimal minimumOrderValue;
    private BigDecimal deliveryCharge;
    private String operatingDays;
    private boolean deliverySupported;
    private LocalTime openingTime;
    private LocalTime closingTime;

    // --- General Info ---
    private Double rating;
    private Integer reviewCount;
    private Integer yearsInBusiness;
    private String businessEmail;
    private String mobileNumber;
    private String gstNumber;

    // --- Products and Media ---
    private List<StorefrontProductDto> products;
    private List<SubCategoryDto> subCategories;

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
        private BigDecimal price;
        private Integer minimumOrderQuantity;
        private Integer bulkDealQuantity;
        private BigDecimal bulkDealPrice;
        private Integer availableStock;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubCategoryDto {
        private String id;
        private String name;
        private String image;
    }
}