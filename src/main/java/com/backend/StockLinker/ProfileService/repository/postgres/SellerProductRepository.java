package com.backend.StockLinker.ProfileService.repository.postgres;

import com.backend.StockLinker.ProfileService.model.MasterProduct;
import com.backend.StockLinker.ProfileService.model.SellerProduct;
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

    List<SellerProduct> findByBusinessProfileId(String businessProfileId);

    long countByBusinessProfileId(String businessProfileId);
    long countByBusinessProfileIdAndAvailableStockLessThan(String businessProfileId, Integer stockLimit);
}