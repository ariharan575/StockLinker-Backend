package com.backend.StockLinker.ProductCatagory_Service.repository;

import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSubCategoryRepository extends JpaRepository<ProductSubCategory, String> {
    List<ProductSubCategory> findByProductCategoryId(String categoryId);
    List<ProductSubCategory> findByProductCategoryIdIn(List<String> categoryIds);

    // FIX: Removed "AndActiveTrue" to match your entity
    List<ProductSubCategory> findTop5ByNameContainingIgnoreCase(String name);

    // NEW FIX: This single query fetches all subcategories AND their parent categories at once
    @Query("SELECT psc FROM ProductSubCategory psc JOIN FETCH psc.productCategory pc WHERE pc.active = true")
    List<ProductSubCategory> findAllActiveWithCategory();
}