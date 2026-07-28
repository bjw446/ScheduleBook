package com.example.schedulebook.domain.notification.repository;

import com.example.schedulebook.domain.notification.entity.NotificationRetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRetryRepository extends JpaRepository<NotificationRetry, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationRetry n SET n.retryStatus = com.example.schedulebook.domain.notification.enums.RetryStatus.SUCCESS, " +
            "n.reason = NULL, n.nextRetryAt = NULL WHERE n.id = :notificationRetryId")
    int markSuccess(@Param("notificationRetryId") Long notificationRetryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationRetry n SET n.retryStatus = com.example.schedulebook.domain.notification.enums.RetryStatus.FAILED, " +
            "n.reason = :reason, n.nextRetryAt = NULL WHERE n.id = :notificationRetryId")
    int markFailed(@Param("notificationRetryId") Long notificationRetryId, @Param("reason") String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationRetry n SET n.retryStatus = com.example.schedulebook.domain.notification.enums.RetryStatus.PENDING, " +
            "n.retryCount = n.retryCount + 1, n.reason = :reason, n.nextRetryAt = :nextRetryAt WHERE n.id = :notificationRetryId")
    int markRetry(@Param("notificationRetryId") Long notificationRetryId,
                  @Param("reason") String reason,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt);

    @Modifying
    @Query("UPDATE NotificationRetry n SET n.retryStatus = com.example.schedulebook.domain.notification.enums.RetryStatus.PROCESSING, " +
            "n.reason = NULL, n.nextRetryAt = NULL WHERE n.id = :notificationRetryId AND " +
            "n.retryStatus = com.example.schedulebook.domain.notification.enums.RetryStatus.PENDING")
    int markProcessing(@Param("notificationRetryId") Long notificationRetryId);

    @Query("SELECT n FROM NotificationRetry n WHERE n.retryStatus = com.example.schedulebook.domain.notification.enums.RetryStatus.PENDING " +
            "OR (n.retryStatus = com.example.schedulebook.domain.notification.enums.RetryStatus.PROCESSING " +
            "AND n.updatedAt <= :timeout) ORDER BY n.retryCount, n.id ASC")
    List<NotificationRetry> findRetryTargets(@Param("timeout") LocalDateTime timeout);
}