package com.backend.StockLinker.onboarding.entity;

import com.backend.StockLinker.onboarding.enums.DeliverySupportType;
import com.backend.StockLinker.onboarding.enums.ProductCategory;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores wholesaler specific business details.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "wholesaler_business_details",
        indexes = {
                @Index(name = "idx_wholesaler_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wholesaler_user_id", columnNames = "user_id")
        }
)
public class WholesalerBusinessDetails extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "wholesaler_product_categories",
            joinColumns = @JoinColumn(name = "wholesaler_details_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", length = 50)
    private Set<ProductCategory> productCategories = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_support", length = 30)
    private DeliverySupportType deliverySupport;
}