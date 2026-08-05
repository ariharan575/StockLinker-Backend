package com.backend.StockLinker.Global_Request_Service.Repository;

import com.backend.StockLinker.Global_Request_Service.Entity.GlobalEnquiry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalEnquiryRepository extends JpaRepository<GlobalEnquiry, String> {

    // Fetch OPEN enquiries for products that this specific wholesaler sells
    @Query("SELECT ge FROM GlobalEnquiry ge WHERE ge.status = 'OPEN' AND ge.masterProduct.id IN (SELECT sp.masterProduct.id FROM SellerProduct sp WHERE sp.businessProfileId = :wholesalerProfileId) ORDER BY ge.createdAt DESC")
    List<GlobalEnquiry> findRelevantEnquiriesForWholesaler(@Param("wholesalerProfileId") String wholesalerProfileId, Pageable pageable);

    // Add this to your existing GlobalEnquiryRepository
    @Query("SELECT COUNT(ge) FROM GlobalEnquiry ge WHERE ge.status = 'OPEN' AND ge.createdAt >= :sinceDate AND ge.masterProduct.id IN (SELECT sp.masterProduct.id FROM SellerProduct sp WHERE sp.businessProfileId = :wholesalerProfileId)")
    long countRelevantEnquiriesSince(@Param("wholesalerProfileId") String wholesalerProfileId, @Param("sinceDate") java.time.LocalDateTime sinceDate);

}