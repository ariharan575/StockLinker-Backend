package com.backend.StockLinker.OrderService.model;

import com.backend.StockLinker.AuthService.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "original_product_id")
    private String originalProductId;

    // --- SNAPSHOT DATA (Copied from SellerProduct at time of order) ---
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "brand")
    private String brand;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "package_size")
    private String packageSize;

    @Column(name = "price_at_purchase", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}