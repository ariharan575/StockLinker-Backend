package com.backend.StockLinker.AuthService.repository;

import com.backend.StockLinker.AuthService.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}