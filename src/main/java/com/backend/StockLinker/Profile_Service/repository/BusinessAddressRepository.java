package com.backend.StockLinker.Profile_Service.repository;

import com.backend.StockLinker.Profile_Service.model.BusinessAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BusinessAddressRepository extends JpaRepository<BusinessAddress, String> {
    Optional<BusinessAddress> findByBusinessProfileId(String businessProfileId);
}