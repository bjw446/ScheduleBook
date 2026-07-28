package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcessedOutboxRepository extends JpaRepository<ProcessedOutbox, Long> {
    Optional<ProcessedOutbox> findByOutboxId(Long outboxId);

    @Modifying
    @Query("UPDATE ProcessedOutbox p SET p.status = com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus.SUCCESS " +
            "WHERE p.outboxId = :outboxId AND p.status = com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus.PROCESSING")
    int markSuccess(@Param("outboxId") Long outboxId);

    @Modifying
    @Query("UPDATE ProcessedOutbox p SET p.status = com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus.FAILED " +
            "WHERE p.outboxId = :outboxId AND p.status = com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus.PROCESSING")
    int markFailed(@Param("outboxId") Long outboxId);

    @Modifying
    @Query("UPDATE ProcessedOutbox p SET p.status = com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus.PROCESSING " +
            "WHERE p.outboxId = :outboxId AND p.status = com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus.FAILED")
    int retry(@Param("outboxId") Long outboxId);
}
