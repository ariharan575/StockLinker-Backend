package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    // =========================================================
    // 🔍 FIND BY ROLE NAME
    // =========================================================
    Optional<Role> findByName(String name);

    // =========================================================
    // ✅ CHECK IF ROLE EXISTS
    // =========================================================
    boolean existsByName(String name);

    // =========================================================
    // 🔍 FIND ROLE WITH PERMISSIONS (EAGER)
    // =========================================================
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

    // =========================================================
    // 🔍 FIND ALL ROLES WITH PERMISSIONS
    // =========================================================
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions")
    Set<Role> findAllWithPermissions();

    // =========================================================
    // 🔍 FIND DEFAULT ROLES (GUEST, USER)
    // =========================================================
    @Query("SELECT r FROM Role r WHERE r.name IN ('GUEST', 'USER')")
    Set<Role> findDefaultRoles();
}