package com.backend.StockLinker.Order_Service.dto;

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