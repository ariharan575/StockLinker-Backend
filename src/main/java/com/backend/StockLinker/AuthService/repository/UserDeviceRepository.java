package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {
    List<UserDevice> findByUserId(String userId);
    Optional<UserDevice> findByUserIdAndDeviceName(String userId, String deviceName);

    Optional<UserDevice> findByDeviceId(String deviceId);

    @Modifying
    @Query("UPDATE UserDevice d SET d.active = false WHERE d.user.id = :userId AND d.id != :currentDeviceId")
    void deactivateOtherDevices(@Param("userId") String userId, @Param("currentDeviceId") String currentDeviceId);

    @Modifying
    @Query("DELETE FROM UserDevice d WHERE d.lastActivityAt < :threshold")
    void deleteInactiveDevices(@Param("threshold") java.time.LocalDateTime threshold);
}
