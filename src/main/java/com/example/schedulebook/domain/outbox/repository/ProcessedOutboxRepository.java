package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOutboxRepository extends JpaRepository<ProcessedOutbox, Long> {
    boolean existsByOutboxId(Long outboxId);
}
