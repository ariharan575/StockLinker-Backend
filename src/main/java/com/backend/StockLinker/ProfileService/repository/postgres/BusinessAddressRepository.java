package com.backend.StockLinker.ProfileService.repository.postgres;

import com.backend.StockLinker.ProfileService.model.BusinessAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BusinessAddressRepository extends JpaRepository<BusinessAddress, String> {
    Optional<BusinessAddress> findByBusinessProfileId(String businessProfileId);
}