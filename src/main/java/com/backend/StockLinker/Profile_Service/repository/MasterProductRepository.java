package com.backend.StockLinker.Profile_Service.repository;

import com.backend.StockLinker.Profile_Service.model.MasterProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterProductRepository extends JpaRepository<MasterProduct, String> {
    List<MasterProduct> findByProductSubCategoryId(String subCategoryId);
    List<MasterProduct> findTop10ByProductNameContainingIgnoreCase(String productName);
}