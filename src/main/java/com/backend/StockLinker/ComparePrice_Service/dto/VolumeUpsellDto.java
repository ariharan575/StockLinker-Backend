package com.backend.StockLinker.ComparePrice_Service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolumeUpsellDto {
    private int rank;
    private String businessProfileId;
    private String businessName;
    private String location;
    private Double rating;
    private int requiredQuantity;
    private BigDecimal bulkTotalPrice;
    private BigDecimal totalSavingsVsMarket;
    private int extraQuantityGained;
}