package com.backend.StockLinker.ProfileService.repository.postgres;

import com.backend.StockLinker.ProfileService.model.MasterProduct;
import com.backend.StockLinker.ProfileService.model.SellerProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerProductRepository extends JpaRepository<SellerProduct, String> {

    List<SellerProduct> findBySellerId(String sellerId);

    List<SellerProduct> findByBusinessProfileId(String businessProfileId);

    List<SellerProduct> findByMasterProduct(MasterProduct masterProduct);

    List<SellerProduct> findByMasterProductId(String masterProductId);

    List<SellerProduct> findByStatus(String status);

    List<SellerProduct> findByBusinessProfileIdAndStatus(
            String businessProfileId,
            String status
    );
}