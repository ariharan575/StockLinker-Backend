package com.backend.StockLinker.onboarding.repository;

import com.backend.StockLinker.onboarding.entity.ShopkeeperBusinessDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for shopkeeper business onboarding details.
 */
@Repository
public interface ShopkeeperBusinessDetailsRepository
        extends JpaRepository<ShopkeeperBusinessDetails, Long> {

    /**
     * Find shopkeeper details by user id.
     *
     * @param userId authenticated user id
     * @return optional shopkeeper details
     */
    Optional<ShopkeeperBusinessDetails> findByUserId(Long userId);

    /**
     * Check shopkeeper details existence by user id.
     *
     * @param userId authenticated user id
     * @return true if exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete shopkeeper details by user id.
     *
     * @param userId authenticated user id
     */
    void deleteByUserId(Long userId);
}