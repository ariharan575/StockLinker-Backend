package com.backend.StockLinker.OrderService.dto;

import com.backend.StockLinker.OrderService.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponseDto {
    private String id;
    private String orderNumber;
    private OrderStatus status;
    private String sellerName;
    private String buyerName;
    private String sellerLocation;
    private BigDecimal totalAmount;
    private Integer totalItems;

    private LocalDate deliveryDate;
    private Integer deliverySequenceNumber;
    private String rejectionReason;

    private LocalDateTime placedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime processingAt;
    private LocalDateTime outForDeliveryAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    private InvoiceDto invoice;
    private List<OrderItemDto> items;

    @Data
    @Builder
    public static class OrderItemDto {
        private String productName;
        private String sku;
        private String packageSize;
        private String unit;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    public static class InvoiceDto {
        private String invoiceNumber;
        private String sellerGstin;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal discount;
        private BigDecimal finalAmount;
    }
}