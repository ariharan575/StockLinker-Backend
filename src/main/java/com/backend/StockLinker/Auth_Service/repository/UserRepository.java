package com.backend.StockLinker.Auth_Service.repository;

import com.backend.StockLinker.Auth_Service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    @Query("SELECT ud.user FROM UserDevice ud WHERE ud.deviceId = :deviceId AND ud.user.provider = 'GUEST'")
    Optional<User> findGuestUserByDeviceId(@Param("deviceId") String deviceId);
}