package com.backend.StockLinker.OrderService.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequestDto {
    private String businessProfileId; // The seller
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private String productId;
        private Integer quantity;
    }
}