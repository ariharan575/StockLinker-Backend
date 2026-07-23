package com.backend.StockLinker.ProfileService.repository.postgres;

import com.backend.StockLinker.ProfileService.model.BusinessProfile;
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
}