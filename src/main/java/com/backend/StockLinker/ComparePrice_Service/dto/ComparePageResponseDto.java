package com.backend.StockLinker.ComparePrice_Service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparePageResponseDto {
    private HeaderMetricsDto headerMetrics;
    private MarketBoundariesDto marketBoundaries;
    private List<VolumeUpsellDto> aiVolumeDeals;
    private List<SupplierDto> suppliers;
}