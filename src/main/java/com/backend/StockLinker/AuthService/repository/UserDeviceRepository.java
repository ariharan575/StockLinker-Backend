package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {

    // =========================================================
    // 🔍 FIND BY DEVICE ID
    // =========================================================
    Optional<UserDevice> findByDeviceId(String deviceId);

    // =========================================================
    // 🔍 FIND ALL DEVICES FOR USER
    // =========================================================
    List<UserDevice> findByUserId(String userId);

    // =========================================================
    // 🔍 FIND ACTIVE DEVICES FOR USER
    // =========================================================
    List<UserDevice> findByUserIdAndActiveTrue(String userId);

    // =========================================================
    // 🔍 FIND TRUSTED DEVICES FOR USER
    // =========================================================
    List<UserDevice> findByUserIdAndTrustedTrue(String userId);

    // =========================================================
    // 🔄 DEACTIVATE ALL USER DEVICES
    // =========================================================
    @Modifying
    @Transactional
    @Query("UPDATE UserDevice d SET d.active = false WHERE d.user.id = :userId")
    void deactivateAllUserDevices(@Param("userId") String userId);

    // =========================================================
    // 🔄 UPDATE LAST ACTIVITY
    // =========================================================
    @Modifying
    @Transactional
    @Query("UPDATE UserDevice d SET d.lastActivityAt = CURRENT_TIMESTAMP, d.ipAddress = :ipAddress WHERE d.deviceId = :deviceId")
    void updateLastActivity(@Param("deviceId") String deviceId, @Param("ipAddress") String ipAddress);
}