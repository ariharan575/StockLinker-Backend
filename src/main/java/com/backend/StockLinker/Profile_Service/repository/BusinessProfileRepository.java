package com.backend.StockLinker.Profile_Service.repository;

import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, String> {
    Optional<BusinessProfile> findByUserId(String userId);

    @Query("SELECT p FROM BusinessProfile p JOIN p.businessAddress a " +
            "WHERE p.businessType = :targetRole " +
            "AND a.district = :userDistrict " +
            "AND p.id != :currentProfileId " +
            "ORDER BY p.trustScore DESC")
    List<BusinessProfile> findNearbyInSameDistrict(
            @Param("userDistrict") String userDistrict,
            @Param("targetRole") String targetRole,
            @Param("currentProfileId") String currentProfileId
    );

    List<BusinessProfile> findTop5ByBusinessNameContainingIgnoreCase(String businessName);

    long countByTrustScoreGreaterThan(Integer trustScore);

    List<BusinessProfile> findTop5ByBusinessNameContainingIgnoreCaseAndBusinessTypeIgnoreCase(String businessName, String businessType);

    // FIXED: Removed CONCAT and LOWER functions around the parameters to prevent PostgreSQL bytea casting errors.
    @Query("SELECT DISTINCT p FROM BusinessProfile p " +
            "LEFT JOIN FETCH p.businessAddress a " +
            "LEFT JOIN FETCH p.deliveryConfiguration d " +
            "WHERE p.id != :currentProfileId " +
            "AND p.businessType = :targetRole " +
            "AND (:scope = 'ALL' OR a.district = :userDistrict OR a.district = 'Universal') " +
            "AND (:search IS NULL OR LOWER(p.businessName) LIKE :search) " +
            "AND (:minRating IS NULL OR p.rating >= :minRating) " +
            "AND (:deliveryRadius IS NULL OR d.coverageRadiusKm >= :deliveryRadius) " +
            "AND (:categoryId IS NULL OR p.categoryIds LIKE :categoryId) " +
            "AND (:filterResponseTime = false OR LOWER(p.responseTime) IN :allowedResponseTimes) " +
            "ORDER BY p.trustScore DESC, p.createdAt DESC")
    Page<BusinessProfile> findNetworkWithFilters(
            @Param("currentProfileId") String currentProfileId,
            @Param("targetRole") String targetRole,
            @Param("scope") String scope,
            @Param("userDistrict") String userDistrict,
            @Param("search") String search,
            @Param("minRating") Double minRating,
            @Param("deliveryRadius") Integer deliveryRadius,
            @Param("categoryId") String categoryId,
            @Param("filterResponseTime") boolean filterResponseTime,
            @Param("allowedResponseTimes") List<String> allowedResponseTimes,
            Pageable pageable
    );
}