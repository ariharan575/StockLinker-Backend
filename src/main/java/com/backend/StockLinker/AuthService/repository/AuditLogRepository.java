package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.enums.AuditAction;
import com.backend.StockLinker.AuthService.enums.ResourceType;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUser(User user, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findByResourceType(ResourceType resourceType, Pageable pageable);

    Page<AuditLog> findByStatus(AuditLog.Status status, Pageable pageable);

    // Assumes BaseEntity provides a "createdAt" field for date range queries
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<AuditLog> findByUserAndActionAndCreatedAtBetween(
            User user, AuditAction action, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}