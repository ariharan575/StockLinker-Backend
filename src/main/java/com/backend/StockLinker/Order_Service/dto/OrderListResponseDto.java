package com.backend.StockLinker.Order_Service.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OrderListResponseDto {
    private String userRole;
    private List<OrderResponseDto> orders;
}