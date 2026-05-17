package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    // =========================================================
    // 🔍 FIND BY PERMISSION NAME
    // =========================================================
    Optional<Permission> findByName(String name);

    // =========================================================
    // ✅ CHECK IF PERMISSION EXISTS
    // =========================================================
    boolean existsByName(String name);

    // =========================================================
    // 🔍 FIND BY RESOURCE
    // =========================================================
    Set<Permission> findByResource(String resource);

    // =========================================================
    // 🔍 FIND BY RESOURCE AND ACTION
    // =========================================================
    Optional<Permission> findByResourceAndAction(String resource, String action);

    // =========================================================
    // 🔍 SEARCH PERMISSIONS BY NAME CONTAINING
    // =========================================================
    @Query("SELECT p FROM Permission p WHERE p.name LIKE %:keyword%")
    Set<Permission> searchByName(@Param("keyword") String keyword);
}