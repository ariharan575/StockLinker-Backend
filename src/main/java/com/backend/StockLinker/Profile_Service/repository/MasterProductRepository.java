package com.backend.StockLinker.Profile_Service.repository;

import com.backend.StockLinker.Profile_Service.model.MasterProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterProductRepository extends JpaRepository<MasterProduct, String> {

    // PHASE 2 FIX: Instantly count valid products for the daily rotation logic (No Java loops)
    @Query("SELECT COUNT(DISTINCT m.id) FROM MasterProduct m INNER JOIN SellerProduct sp ON sp.masterProduct.id = m.id WHERE sp.status = 'ACTIVE'")
    long countProductsWithActiveSellers();

    // PHASE 2 FIX: Fetch only products that actually have active sellers, strictly limited by Pageable
    @Query("SELECT DISTINCT m FROM MasterProduct m INNER JOIN SellerProduct sp ON sp.masterProduct.id = m.id WHERE sp.status = 'ACTIVE' ORDER BY m.id ASC")
    Page<MasterProduct> findProductsWithActiveSellers(Pageable pageable);

    List<MasterProduct> findTop10ByProductNameContainingIgnoreCase(String productName);
}