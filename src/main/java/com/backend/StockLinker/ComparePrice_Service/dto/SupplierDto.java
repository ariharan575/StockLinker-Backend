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
public class SupplierDto {
    private String id;
    private String businessProfileId;
    private String businessName;
    private String initials;
    private String locationDistrict;

    private int moq;
    private String unit;
    private BigDecimal basePricePerUnit;

    private int requestedQuantity;
    private BigDecimal calculatedTotalPrice;

    private Integer bulkQty;
    private BigDecimal bulkTotalPrice; // Total flat price for the bundle
    private BigDecimal bulkSavingsAmount;

    private Double rating;
    private Integer trustScore;
    private Integer availableStock;
    private boolean verified;
}