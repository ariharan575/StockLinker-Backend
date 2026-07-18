package com.backend.StockLinker.ProfileService.model;

import com.backend.StockLinker.AuthService.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(
        name = "seller_products",
        indexes = {
                @Index(name = "idx_sp_seller", columnList = "seller_id"),
                @Index(name = "idx_sp_business_profile", columnList = "business_profile_id"),
                @Index(name = "idx_sp_master_product", columnList = "master_product_id"),
                @Index(name = "idx_sp_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SellerProduct extends BaseEntity {

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "business_profile_id", nullable = false)
    private String businessProfileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_product_id", nullable = false)
    private MasterProduct masterProduct;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "package_size", length = 100)
    private String packageSize;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "minimum_order_quantity", nullable = false)
    private Integer minimumOrderQuantity;

    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}