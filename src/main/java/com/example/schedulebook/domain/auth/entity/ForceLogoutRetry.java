package com.example.schedulebook.domain.auth.entity;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.entity.ModifyEntity;
import com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "force_logout_retry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ForceLogoutRetry extends ModifyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "force_logout_retry_status", nullable = false)
    private ForceLogoutRetryStatus forceLogoutRetryStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "claim_token")
    private String claimToken;

    public static ForceLogoutRetry create(String eventId, String sessionId, Long userId, String payload, String reason) {
        ForceLogoutRetry forceLogoutRetry = new ForceLogoutRetry();

        forceLogoutRetry.eventId = eventId;
        forceLogoutRetry.sessionId = sessionId;
        forceLogoutRetry.userId = userId;
        forceLogoutRetry.payload = payload;
        forceLogoutRetry.forceLogoutRetryStatus = ForceLogoutRetryStatus.PENDING;
        forceLogoutRetry.retryCount = 0;
        forceLogoutRetry.nextRetryAt = LocalDateTime.now().plusSeconds(CommonConst.NEXT_RETRY_DELAY);
        forceLogoutRetry.reason = reason;

        return forceLogoutRetry;
    }
}
