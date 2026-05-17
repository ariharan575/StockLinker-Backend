package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // =========================================================
    // 🔍 FIND BY EMAIL
    // =========================================================
    Optional<User> findByEmail(String email);

    // =========================================================
    // 🔍 FIND BY PHONE NUMBER
    // =========================================================
    Optional<User> findByPhone(String phone);

    // =========================================================
    // 🔍 FIND BY UNIQUE ID (OAUTH PROVIDER ID)
    // =========================================================
    Optional<User> findByUniqueId(String uniqueId);

    // =========================================================
    // ✅ CHECK IF USER EXISTS BY EMAIL
    // =========================================================
    boolean existsByEmail(String email);

    // =========================================================
    // ✅ CHECK IF USER EXISTS BY PHONE
    // =========================================================
    boolean existsByPhone(String phone);

    // =========================================================
    // 🔍 FIND USER WITH ROLES (EAGER FETCH)
    // =========================================================
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :userId")
    Optional<User> findByIdWithRoles(@Param("userId") String userId);

    // =========================================================
    // 🔍 FIND USER WITH DEVICES
    // =========================================================
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.devices WHERE u.id = :userId")
    Optional<User> findByIdWithDevices(@Param("userId") String userId);

    // =========================================================
    // 🔍 FIND USER WITH ALL RELATIONS
    // =========================================================
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "LEFT JOIN FETCH u.devices d " +
            "WHERE u.id = :userId")
    Optional<User> findByIdWithAllRelations(@Param("userId") String userId);
}