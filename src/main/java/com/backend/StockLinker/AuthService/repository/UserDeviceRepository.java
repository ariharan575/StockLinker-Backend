package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {

    Optional<UserDevice> findByUserIdAndDeviceId(String userId, String deviceId);

    // Added for cross-account device collision protection
    Optional<UserDevice> findByDeviceId(String deviceId);

}