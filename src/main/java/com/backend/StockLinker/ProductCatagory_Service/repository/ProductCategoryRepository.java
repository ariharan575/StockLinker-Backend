package com.backend.StockLinker.ProductCatagory_Service.repository;

import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {
    List<ProductCategory> findByActiveTrue();
    List<ProductCategory> findTop5ByNameContainingIgnoreCaseAndActiveTrue(String name);
}