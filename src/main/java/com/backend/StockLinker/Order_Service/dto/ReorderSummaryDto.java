package com.backend.StockLinker.Order_Service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSummaryDto {
    private String orderId;
    private String orderNumber;
    private LocalDate date;
    private String sellerName;
    private String sellerBusinessProfileId;
    private String masterProductId; // Used for routing to the Compare Price page
    private List<String> items;
    private BigDecimal previousPrice;
    private BigDecimal currentPrice;
    private BigDecimal priceDifference;
}