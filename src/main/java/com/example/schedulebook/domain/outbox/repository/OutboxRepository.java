package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    @Query(value = "SELECT * FROM outbox WHERE status = 'PENDING' OR (status = 'FAILED' AND " +
            "next_retry_at <= :now) ORDER BY id FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Outbox> findRetryTargets(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT o FROM Outbox o WHERE o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.PROCESSING " +
            "AND o.processingAt < :expired")
    List<Outbox> findStuckOutboxes(@Param("expired") LocalDateTime expired);
}
