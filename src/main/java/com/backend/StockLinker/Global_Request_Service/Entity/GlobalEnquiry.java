package com.backend.StockLinker.Global_Request_Service.Entity;

import com.backend.StockLinker.Profile_Service.model.MasterProduct;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_enquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalEnquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String buyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_id", nullable = false)
    @JsonIgnore
    private MasterProduct masterProduct;

    @Column(nullable = false)
    private Integer requestedQuantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private String status; // OPEN, CLOSED, FULFILLED

    @CreationTimestamp
    private LocalDateTime createdAt;

    // FIX: Added to resolve the null constraint error
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}