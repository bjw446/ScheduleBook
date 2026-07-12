package com.example.schedulebook.domain.auth.repository;

import com.example.schedulebook.domain.auth.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
}
