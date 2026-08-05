package com.backend.StockLinker.Profile_Service.model;

import com.backend.StockLinker.Auth_Service.model.BaseEntity;
import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductSubCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "master_products", indexes = {
        @Index(name = "idx_mp_name", columnList = "product_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MasterProduct extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_sub_category_id", nullable = false)
    private ProductSubCategory productSubCategory;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

}