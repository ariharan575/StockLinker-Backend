package com.backend.StockLinker.onboarding.repository;

import com.backend.StockLinker.onboarding.entity.WholesalerBusinessDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for wholesaler business onboarding details.
 */
@Repository
public interface WholesalerBusinessDetailsRepository
        extends JpaRepository<WholesalerBusinessDetails, Long> {

    /**
     * Find wholesaler details by user id.
     *
     * @param userId authenticated user id
     * @return optional wholesaler details
     */
    Optional<WholesalerBusinessDetails> findByUserId(Long userId);

    /**
     * Check wholesaler details existence by user id.
     *
     * @param userId authenticated user id
     * @return true if exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete wholesaler details by user id.
     *
     * @param userId authenticated user id
     */
    void deleteByUserId(Long userId);
}