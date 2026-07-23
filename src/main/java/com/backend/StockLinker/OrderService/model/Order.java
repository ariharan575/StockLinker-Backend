package com.backend.StockLinker.OrderService.model;

import com.backend.StockLinker.AuthService.model.BaseEntity;
import com.backend.StockLinker.OrderService.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_buyer", columnList = "buyer_id"),
        @Index(name = "idx_order_seller", columnList = "seller_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_delivery_date", columnList = "delivery_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_sequence_number")
    private Integer deliverySequenceNumber;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Version
    @Column(name = "version")
    private Long version;

    // --- Timeline Tracking Timestamps ---
    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @Column(name = "confirmed_at") // Marks transition from PENDING to PROCESSING
    private LocalDateTime confirmedAt;

    @Column(name = "out_for_delivery_at")
    private LocalDateTime outForDeliveryAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Invoice invoice;

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }
}