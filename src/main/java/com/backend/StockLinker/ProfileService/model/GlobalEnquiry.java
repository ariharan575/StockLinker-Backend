package com.backend.StockLinker.ProfileService.model;

import com.backend.StockLinker.AuthService.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "global_enquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GlobalEnquiry extends BaseEntity {

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_id", nullable = false)
    private MasterProduct masterProduct;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "target_price", precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // e.g., "OPEN", "FULFILLED", "CLOSED"
}