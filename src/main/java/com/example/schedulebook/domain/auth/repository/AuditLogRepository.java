package com.example.schedulebook.domain.auth.repository;

import com.example.schedulebook.domain.auth.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    boolean existsByOutboxId(Long outboxId);
}
