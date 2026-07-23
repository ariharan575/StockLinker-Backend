package com.backend.StockLinker.ProfileService.dto.request;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public class ProfileDTO {

    @Data
    @Builder
    public static class SubCategoryDto {
        private String id;
        private String name;
        private String imageName;
    }

    @Data
    @Builder
    public static class FullProfileResponse {
        private String userId;
        private String ownerName;
        private String businessName;
        private String mobileNumber;
        private String businessEmail;
        private String alternateMobileNumber;
        private String businessType;
        private String gstNumber;
        private Integer yearsInBusiness;
        private LocalTime openingTime;
        private LocalTime closingTime;
        private String verificationStatus;
        private Integer trustScore;
        private Integer marketplaceRank;

        // Address
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String district;
        private String state;
        private String pincode;
        private String landmark;

        // Delivery
        private Integer coverageRadiusKm;
        private BigDecimal minimumOrderValue;
        private BigDecimal deliveryCharge;
        private String operatingDays;
        private String routeSchedule;

        // Inventory & Products
        private List<SubCategoryDto> subCategories;
        private Long totalProducts;
        private Long lowStockCount;
        private String bestSellingProduct;
        private String fastMovingCategory;
    }

    @Data
    public static class AccountUpdateRequest {
        private String ownerName;
        private String mobileNumber;
        private String businessEmail;
    }

    @Data
    public static class BusinessUpdateRequest {
        private String businessName;
        private String businessType;
        private String gstNumber;
        private Integer yearsInBusiness;
        private LocalTime openingTime;
        private LocalTime closingTime;
        private String alternateMobileNumber;
    }

    @Data
    public static class DeliveryUpdateRequest {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String district;
        private String state;
        private String pincode;
        private String landmark;
        private Integer coverageRadiusKm;
        private BigDecimal minimumOrderValue;
        private BigDecimal deliveryCharge;
        private String operatingDays;
        private String routeSchedule;
    }
}