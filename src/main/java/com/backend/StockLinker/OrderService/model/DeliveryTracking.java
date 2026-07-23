package com.backend.StockLinker.OrderService.model;

import com.backend.StockLinker.AuthService.model.BaseEntity;
import com.backend.StockLinker.OrderService.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_tracking", indexes = {
        @Index(name = "idx_dt_seller_date", columnList = "seller_id, scheduled_date"),
        @Index(name = "idx_dt_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeliveryTracking extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private OrderStatus deliveryStatus;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}