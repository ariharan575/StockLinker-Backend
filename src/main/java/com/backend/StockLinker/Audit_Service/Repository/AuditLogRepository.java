package com.backend.StockLinker.Audit_Service.Repository;

import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
}