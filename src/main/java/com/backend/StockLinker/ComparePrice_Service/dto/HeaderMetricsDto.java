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
public class HeaderMetricsDto {
    private String masterProductId;
    private String productName;
    private String category;
    private int supplierCount;
    private BigDecimal bestPriceTotal;
    private BigDecimal marketAverageTotal;

    // ADD THIS LINE TO FIX THE ERROR
    private BigDecimal totalSavings;

    // The top 3 suppliers to power the Supplier Matrix Progress Bars in the Header
    private List<SupplierMatrixDto> top3Matrix;
}