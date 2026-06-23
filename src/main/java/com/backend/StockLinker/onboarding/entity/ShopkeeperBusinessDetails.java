package com.backend.StockLinker.onboarding.entity;

import com.backend.StockLinker.onboarding.enums.PurchaseCategory;
import com.backend.StockLinker.onboarding.enums.StoreType;
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
 * Stores shopkeeper specific business details.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "shopkeeper_business_details",
        indexes = {
                @Index(name = "idx_shopkeeper_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shopkeeper_user_id", columnNames = "user_id")
        }
)
public class ShopkeeperBusinessDetails extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "shopkeeper_purchase_categories",
            joinColumns = @JoinColumn(name = "shopkeeper_details_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_category", length = 50)
    private Set<PurchaseCategory> purchaseCategories = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "store_type", length = 50)
    private StoreType storeType;
}
