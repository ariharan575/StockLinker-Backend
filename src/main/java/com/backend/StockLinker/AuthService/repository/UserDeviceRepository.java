package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {

    Optional<UserDevice> findByDeviceId(String deviceId);

    Optional<UserDevice> findByUserIdAndDeviceId(String userId, String deviceId);

    List<UserDevice> findByUserId(String userId);

    List<UserDevice> findByUserIdAndActiveTrue(String userId);

    List<UserDevice> findByUserIdOrderByLastActivityAtDesc(String userId);

    List<UserDevice> findByUserIdAndTrustedTrue(String userId);

    boolean existsByUserIdAndDeviceId(String userId, String deviceId);

    long countByUserId(String userId);

    long countByUserIdAndActiveTrue(String userId);
}