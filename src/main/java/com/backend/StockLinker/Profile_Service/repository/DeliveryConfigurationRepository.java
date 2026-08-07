package com.backend.StockLinker.Profile_Service.repository;

import com.backend.StockLinker.Profile_Service.model.DeliveryConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DeliveryConfigurationRepository extends JpaRepository<DeliveryConfiguration, String> {
    Optional<DeliveryConfiguration> findByBusinessProfileId(String businessProfileId);
}