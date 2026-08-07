package com.backend.StockLinker.Audit_Service.Repository;

import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
}