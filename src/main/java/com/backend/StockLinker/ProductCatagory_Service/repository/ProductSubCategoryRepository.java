package com.backend.StockLinker.ProductCatagory_Service.repository;

import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSubCategoryRepository extends JpaRepository<ProductSubCategory, String> {
    List<ProductSubCategory> findByProductCategoryId(String categoryId);
    List<ProductSubCategory> findByProductCategoryIdIn(List<String> categoryIds);
}