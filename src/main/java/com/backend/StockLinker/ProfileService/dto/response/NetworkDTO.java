package com.backend.StockLinker.ProfileService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

public class NetworkDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class WsNotification {
        private String type; // "NEW_REQUEST", "ACCEPTED", "NEW_NEARBY_USER"
        private String message;
        private NetworkMemberResponse payload;
    }

    @Data
    @Builder
    public static class SubCategoryMiniDto {
        private String name;
        private String image;
    }

    @Data
    @Builder
    public static class NetworkMemberResponse {
        private String id;
        private String userId;
        private String connectionId;
        private String name;
        private String category;
        private String location;
        private String distance;
        private Double rating;
        private Integer reviews;

        private List<String> verification;
        private String experience;
        private String orders;
        private String responseTime;
        private String status;
        private String avatar;
        private Boolean readyStock;

        private String deliveryRadius;
        private String deliveryEstimate;

        private List<SubCategoryMiniDto> subCategories;
        private Long totalSubCategories;

        private String connectionStatus;
    }
}