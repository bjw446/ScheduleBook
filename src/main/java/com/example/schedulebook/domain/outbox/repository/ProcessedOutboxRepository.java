package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedOutboxRepository extends JpaRepository<ProcessedOutbox, Long> {
    Optional<ProcessedOutbox> findByOutboxId(Long outboxId);
}
