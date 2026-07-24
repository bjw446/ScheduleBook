package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    @Query(value = "SELECT * FROM outbox WHERE status = 'PENDING' OR (status = 'FAILED' AND next_retry_at <= :now) " +
            "ORDER BY id LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Outbox> findRetryTargets(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);

    @Query(value = "SELECT * FROM outbox WHERE status = 'PROCESSING' AND processing_at < :expired " +
            "ORDER BY id LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Outbox> findStuckOutboxes(@Param("expired") LocalDateTime expired, @Param("batchSize") int batchSize);
}
