package com.example.schedulebook.domain.auth.repository;

import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ForceLogoutRetryRepository extends JpaRepository<ForceLogoutRetry, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ForceLogoutRetry f SET f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.SUCCESS, " +
            "f.reason = NULL, f.nextRetryAt = NULL, f.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE f.id = :forceLogoutRetryId AND f.claimToken = :claimToken")
    int markSuccess(@Param("forceLogoutRetryId") Long forceLogoutRetryId, @Param("claimToken") String claimToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ForceLogoutRetry f SET f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.FAILED, " +
            "f.reason = :reason, f.nextRetryAt = NULL, f.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE f.id = :forceLogoutRetryId AND f.claimToken = :claimToken")
    int markFailed(@Param("forceLogoutRetryId") Long forceLogoutRetryId, @Param("reason") String reason,
                   @Param("claimToken") String claimToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ForceLogoutRetry f SET f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.PENDING, " +
            "f.retryCount = f.retryCount + 1, f.reason = :reason, f.nextRetryAt = :nextRetryAt, " +
            "f.updatedAt = CURRENT_TIMESTAMP WHERE f.id = :forceLogoutRetryId AND f.claimToken = :claimToken")
    int markRetry(@Param("forceLogoutRetryId") Long forceLogoutRetryId,
                  @Param("reason") String reason,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt,
                  @Param("claimToken") String claimToken);

    @Modifying
    @Query("UPDATE ForceLogoutRetry f SET f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.PROCESSING, " +
            "f.claimToken = :claimToken, f.reason = NULL, f.nextRetryAt = NULL, f.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE f.id = :forceLogoutRetryId AND ((f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.PENDING " +
            "AND (f.nextRetryAt IS NULL OR f.nextRetryAt <= CURRENT_TIMESTAMP)) OR (f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.PROCESSING " +
            "AND f.updatedAt <= :timeout))")
    int markProcessing(@Param("forceLogoutRetryId") Long forceLogoutRetryId,
                       @Param("claimToken") String claimToken,
                       @Param("timeout") LocalDateTime timeout);

    @Query("SELECT f FROM ForceLogoutRetry f WHERE (f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.PENDING " +
            "AND (f.nextRetryAt IS NULL OR f.nextRetryAt <= CURRENT_TIMESTAMP)) OR (f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.PROCESSING " +
            "AND f.updatedAt <= :timeout) ORDER BY f.nextRetryAt, f.retryCount, f.id ASC")
    Page<ForceLogoutRetry> findRetryTargets(@Param("timeout") LocalDateTime timeout, Pageable pageable);

    @Modifying
    @Query("UPDATE ForceLogoutRetry f SET f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.PENDING, " +
            "f.retryCount = 0, f.reason = NULL, f.nextRetryAt = NULL, f.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE f.id = :forceLogoutRetryId AND f.claimToken = :claimToken ")
    int updateRecover(@Param("forceLogoutRetryId") Long forceLogoutRetryId, @Param("claimToken") String claimToken);

    Optional<ForceLogoutRetry> findBySessionId(String sessionId);

    @Query("SELECT COUNT(f) FROM ForceLogoutRetry f WHERE f.forceLogoutRetryStatus = " +
            "com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus.PENDING")
    long countPending();
}
