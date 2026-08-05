package com.backend.StockLinker.Profile_Service.repository;

import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SellerProductRepository extends JpaRepository<SellerProduct, String>, JpaSpecificationExecutor<SellerProduct> {

    @Query("SELECT DISTINCT s.brand FROM SellerProduct s WHERE s.sellerId = :sellerId AND s.brand IS NOT NULL")
    List<String> findDistinctBrandsBySellerId(@Param("sellerId") String sellerId);

    @Query("SELECT DISTINCT s.masterProduct.productSubCategory.productCategory.name FROM SellerProduct s WHERE s.sellerId = :sellerId")
    List<String> findDistinctCategoriesBySellerId(@Param("sellerId") String sellerId);

    @Query("SELECT sp FROM SellerProduct sp WHERE sp.masterProduct.id = :masterProductId AND sp.status = 'ACTIVE'")
    List<SellerProduct> findActiveByMasterProductId(@Param("masterProductId") String masterProductId);

    List<SellerProduct> findByBusinessProfileId(String businessProfileId);

    long countByBusinessProfileId(String businessProfileId);

    long countByBusinessProfileIdAndAvailableStockLessThan(String businessProfileId, Integer stockLimit);

    // Added this new query to count sellers for a specific category
    @Query("SELECT COUNT(DISTINCT sp.sellerId) FROM SellerProduct sp WHERE sp.masterProduct.productSubCategory.productCategory.id = :categoryId AND sp.status = 'ACTIVE'")
    long countDistinctSellersByCategoryId(@Param("categoryId") String categoryId);

    // Add this to your existing SellerProductRepository
    long countByBusinessProfileIdAndStatus(String businessProfileId, String status);
}