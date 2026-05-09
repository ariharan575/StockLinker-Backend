package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") String id);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = :attempts WHERE u.id = :userId")
    void updateFailedAttempts(@Param("userId") String userId, @Param("attempts") int attempts);

    @Modifying
    @Query("UPDATE User u SET u.accountLocked = :locked WHERE u.id = :userId")
    void updateAccountLocked(@Param("userId") String userId, @Param("locked") boolean locked);
}