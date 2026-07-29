package com.example.schedulebook.domain.notificationretry.repository;

import com.example.schedulebook.domain.notificationretry.entity.ProcessedNotificationRetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcessedNotificationRetryRepository extends JpaRepository<ProcessedNotificationRetry, Long> {
    Optional<ProcessedNotificationRetry> findByOutboxIdAndReceiverId(Long outboxId, Long receiverId);

    @Modifying
    @Query("UPDATE ProcessedNotificationRetry p SET p.status = " +
            "com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus.SUCCESS " +
            "WHERE p.outboxId = :outboxId AND p.receiverId = :receiverId AND p.status = " +
            "com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus.PROCESSING")
    int markSuccess(@Param("outboxId") Long outboxId, @Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE ProcessedNotificationRetry p SET p.status = " +
            "com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus.PROCESSING " +
            "WHERE p.outboxId = :outboxId AND p.receiverId = :receiverId AND p.status = " +
            "com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus.FAILED")
    int markRetry(@Param("outboxId") Long outboxId, @Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE ProcessedNotificationRetry p SET p.status = " +
            "com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus.FAILED " +
            "WHERE p.outboxId = :outboxId AND p.receiverId = :receiverId AND p.status = " +
            "com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus.PROCESSING")
    int markFailed(@Param("outboxId") Long outboxId, @Param("receiverId") Long receiverId);
}
