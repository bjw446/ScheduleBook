package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query("UPDATE Outbox o SET o.status = :outboxStatus, o.publishedAt = :publishedAt, " +
            "o.processingAt = NULL, o.errorMessage = NULL WHERE o.id = :outboxId " +
            "AND o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.PROCESSING")
    int updateStatusIfProcessing(@Param("outboxId") Long outboxId,
                                 @Param("outboxStatus") OutboxStatus outboxStatus,
                                 @Param("publishedAt") LocalDateTime publishedAt);

    @Modifying
    @Query("UPDATE Outbox o SET o.status = :outboxStatus, o.errorMessage = :errorMessage, o.nextRetryAt = :nextRetryAt, " +
            "o.retryCount = o.retryCount + 1, o.processingAt = NULL WHERE o.id = :outboxId " +
            "AND o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.PROCESSING")
    int updateFailureIfProcessing(@Param("outboxId") Long outboxId,
                                  @Param("outboxStatus") OutboxStatus outboxStatus,
                                  @Param("errorMessage") String errorMessage,
                                  @Param("nextRetryAt") LocalDateTime nextRetryAt);

    @Modifying
    @Query("UPDATE Outbox o SET o.status = :outboxStatus, o.errorMessage = :errorMessage, o.nextRetryAt = :nextRetryAt, " +
            "o.retryCount = o.retryCount + 1, o.processingAt = NULL WHERE o.id = :outboxId " +
            "AND o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.PROCESSING")
    int updateRecoverIfProcessing(@Param("outboxId") Long outboxId,
                                  @Param("outboxStatus") OutboxStatus outboxStatus,
                                  @Param("errorMessage") String errorMessage,
                                  @Param("nextRetryAt") LocalDateTime nextRetryAt);


    Page<Outbox> findAllByStatus(OutboxStatus outboxStatus, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Outbox o WHERE o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.SUCCESS " +
            "AND o.publishedAt < :target")
    int deleteSuccessBefore(@Param("target") LocalDateTime target);

    @Query("SELECT " +
            "SUM(CASE WHEN o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.PENDING THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.PROCESSING THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.FAILED THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.DEAD THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.SUCCESS THEN 1 ELSE 0 END) " +
            "FROM Outbox o")
    Object[] countStats();
}
