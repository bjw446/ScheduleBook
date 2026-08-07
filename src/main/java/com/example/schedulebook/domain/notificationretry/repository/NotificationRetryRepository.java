package com.example.schedulebook.domain.notificationretry.repository;

import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRetryRepository extends JpaRepository<NotificationRetry, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationRetry n SET n.notificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.SUCCESS, " +
            "n.reason = NULL, n.nextRetryAt = NULL, n.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE n.id = :notificationRetryId AND n.claimToken = :claimToken")
    int markSuccess(@Param("notificationRetryId") Long notificationRetryId, @Param("claimToken") String claimToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationRetry n SET n.notificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.FAILED, " +
            "n.reason = :reason, n.nextRetryAt = NULL, n.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE n.id = :notificationRetryId AND n.claimToken = :claimToken")
    int markFailed(@Param("notificationRetryId") Long notificationRetryId, @Param("reason") String reason,
                   @Param("claimToken") String claimToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationRetry n SET n.notificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.PENDING, " +
            "n.retryCount = n.retryCount + 1, n.reason = :reason, n.nextRetryAt = :nextRetryAt, " +
            "n.updatedAt = CURRENT_TIMESTAMP WHERE n.id = :notificationRetryId AND n.claimToken = :claimToken")
    int markRetry(@Param("notificationRetryId") Long notificationRetryId,
                  @Param("reason") String reason,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt,
                  @Param("claimToken") String claimToken);

    @Modifying
    @Query("UPDATE NotificationRetry n SET n.notificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.PROCESSING, " +
            "n.claimToken = :claimToken, n.reason = NULL, n.nextRetryAt = NULL, n.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE n.id = :notificationRetryId AND ((n.notificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.PENDING " +
            "AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= CURRENT_TIMESTAMP)) OR (n.notificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.PROCESSING " +
            "AND n.updatedAt <= :timeout))")
    int markProcessing(@Param("notificationRetryId") Long notificationRetryId,
                       @Param("claimToken") String claimToken,
                       @Param("timeout") LocalDateTime timeout);

    @Query("SELECT n FROM NotificationRetry n WHERE (n.notificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.PENDING " +
            "AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= CURRENT_TIMESTAMP)) OR (n.notificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.PROCESSING " +
            "AND n.updatedAt <= :timeout) ORDER BY n.nextRetryAt, n.retryCount, n.id ASC")
    Page<NotificationRetry> findRetryTargets(@Param("timeout") LocalDateTime timeout, Pageable pageable);

    @Query("SELECT COUNT(n) FROM NotificationRetry f WHERE n.NotificationRetryStatus = " +
            "com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus.PENDING")
    long countPending();
}