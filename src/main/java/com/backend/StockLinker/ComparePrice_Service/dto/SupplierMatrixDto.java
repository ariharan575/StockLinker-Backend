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
public class SupplierMatrixDto {
    private String businessName;
    private BigDecimal calculatedTotalPrice;
    private BigDecimal differenceFromAverage;
    private int percentageDifference;
    private String comparisonStatus; // "CHEAPER", "NEUTRAL", "HIGHER"
}