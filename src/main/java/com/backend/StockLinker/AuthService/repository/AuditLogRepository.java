package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // =========================================================
    // 🔍 FIND BY USER ID
    // =========================================================
    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    // =========================================================
    // 🔍 FIND BY ACTION
    // =========================================================
    Page<AuditLog> findByAction(String action, Pageable pageable);

    // =========================================================
    // 🔍 FIND BY STATUS
    // =========================================================
    Page<AuditLog> findByStatus(AuditLog.Status status, Pageable pageable);

    // =========================================================
    // 🔍 FIND BY DATE RANGE
    // =========================================================
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);

    // =========================================================
    // 🔍 FIND RECENT LOGS FOR USER
    // =========================================================
    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentByUserId(@Param("userId") String userId, Pageable pageable);

    // =========================================================
    // 📊 COUNT BY ACTION
    // =========================================================
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    List<Object[]> countByAction();

    // =========================================================
    // 📊 COUNT BY STATUS AND DATE
    // =========================================================
    @Query("SELECT a.status, COUNT(a) FROM AuditLog a WHERE a.createdAt >= :since GROUP BY a.status")
    List<Object[]> countByStatusSince(@Param("since") LocalDateTime since);
}