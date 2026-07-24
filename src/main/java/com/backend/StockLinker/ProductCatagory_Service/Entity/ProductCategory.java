package com.backend.StockLinker.ProductCatagory_Service.Entity;

import com.backend.StockLinker.Auth_Service.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "product_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductCategory extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Column(name = "icon")
    private String icon;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}