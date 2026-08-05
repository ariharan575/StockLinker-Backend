package com.backend.StockLinker.Global_Request_Service.Dto;

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
public class EnquiryResponseDto {
    private String id;
    private String buyer;
    private String buyerProfileId;
    private String masterProductId;
    private String avatar;
    private boolean isVerified;
    private String status;
    private String title;
    private List<ChipDto> chips;
    private String message;
    private String location;
    private String distance;
    private String time;
    private BigDecimal targetPrice;
    private Integer requestedQuantity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChipDto {
        private String icon;
        private String label;
    }
}