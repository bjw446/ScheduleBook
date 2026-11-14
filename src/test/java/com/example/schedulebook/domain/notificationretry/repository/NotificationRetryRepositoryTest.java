package com.example.schedulebook.domain.notificationretry.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notificationretry.entity.NotificationRetry;
import com.example.schedulebook.domain.notificationretry.enums.NotificationRetryStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class NotificationRetryRepositoryTest {

    @Autowired
    private NotificationRetryRepository notificationRetryRepository;

    @Autowired
    private EntityManager entityManager;

    private NotificationRetry createNotificationRetry(
            String eventId,
            Long outboxId,
            Long receiverId
    ) {
        return NotificationRetry.create(
                eventId,
                outboxId,
                receiverId,
                NotificationType.FRIEND_REQUEST,
                "{\"receiverId\":" + receiverId + "}",
                "알림 발송 실패"
        );
    }

    private void persist(NotificationRetry... notificationRetries) {
        for (NotificationRetry notificationRetry : notificationRetries) {
            entityManager.persist(notificationRetry);
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 테스트 데이터의 상태 및 재시도 관련 필드를 직접 설정한다.
     *
     * Repository의 JPQL을 독립적으로 검증하기 위한 테스트 데이터 준비용 메서드다.
     */
    private void updateNotificationRetry(
            Long id,
            NotificationRetryStatus status,
            int retryCount,
            LocalDateTime nextRetryAt,
            String reason,
            String claimToken,
            LocalDateTime updatedAt
    ) {
        entityManager.createNativeQuery("""
                UPDATE notification_retry
                SET retry_status = :status,
                    retry_count = :retryCount,
                    next_retry_at = :nextRetryAt,
                    reason = :reason,
                    claim_token = :claimToken,
                    updated_at = :updatedAt
                WHERE id = :id
                """)
                .setParameter("status", status.name())
                .setParameter("retryCount", retryCount)
                .setParameter("nextRetryAt", nextRetryAt)
                .setParameter("reason", reason)
                .setParameter("claimToken", claimToken)
                .setParameter("updatedAt", updatedAt)
                .setParameter("id", id)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("올바른 Claim Token으로 알림 재시도 상태를 SUCCESS로 변경한다")
    void markSuccess() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PROCESSING,
                0,
                LocalDateTime.now(),
                "이전 실패 사유",
                "claim-token-1",
                LocalDateTime.now()
        );

        // when
        int updatedCount = notificationRetryRepository.markSuccess(
                notificationRetry.getId(),
                "claim-token-1"
        );

        // then
        Optional<NotificationRetry> result =
                notificationRetryRepository.findById(notificationRetry.getId());

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result).isPresent();
        assertThat(result.get().getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.SUCCESS);
        assertThat(result.get().getReason()).isNull();
        assertThat(result.get().getNextRetryAt()).isNull();
        assertThat(result.get().getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 SUCCESS 상태로 변경하지 않는다")
    void markSuccessWhenClaimTokenIsDifferent() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PROCESSING,
                0,
                null,
                "기존 실패 사유",
                "claim-token-1",
                LocalDateTime.now()
        );

        // when
        int updatedCount = notificationRetryRepository.markSuccess(
                notificationRetry.getId(),
                "wrong-token"
        );

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PROCESSING);
        assertThat(result.getReason()).isEqualTo("기존 실패 사유");
        assertThat(result.getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("올바른 Claim Token으로 알림 재시도 상태를 FAILED로 변경한다")
    void markFailed() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PROCESSING,
                0,
                null,
                null,
                "claim-token-1",
                LocalDateTime.now()
        );

        // when
        int updatedCount = notificationRetryRepository.markFailed(
                notificationRetry.getId(),
                "알림 발송 실패",
                "claim-token-1"
        );

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.FAILED);
        assertThat(result.getReason()).isEqualTo("알림 발송 실패");
        assertThat(result.getNextRetryAt()).isNull();
        assertThat(result.getClaimToken()).isNull();
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 FAILED 상태로 변경하지 않는다")
    void markFailedWhenClaimTokenIsDifferent() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PROCESSING,
                0,
                null,
                null,
                "claim-token-1",
                LocalDateTime.now()
        );

        // when
        int updatedCount = notificationRetryRepository.markFailed(
                notificationRetry.getId(),
                "알림 발송 실패",
                "wrong-token"
        );

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PROCESSING);
        assertThat(result.getReason()).isNull();
        assertThat(result.getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("올바른 Claim Token으로 재시도 대기 상태로 변경한다")
    void markRetry() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PROCESSING,
                2,
                null,
                null,
                "claim-token-1",
                LocalDateTime.now()
        );

        LocalDateTime nextRetryAt = LocalDateTime.now()
                .plusMinutes(10)
                .withNano(0);

        // when
        int updatedCount = notificationRetryRepository.markRetry(
                notificationRetry.getId(),
                "일시적인 발송 오류",
                nextRetryAt,
                "claim-token-1"
        );

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(3);
        assertThat(result.getReason()).isEqualTo("일시적인 발송 오류");
        assertThat(result.getNextRetryAt()).isEqualTo(nextRetryAt);
        assertThat(result.getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 재시도 대기 상태로 변경하지 않는다")
    void markRetryWhenClaimTokenIsDifferent() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PROCESSING,
                2,
                null,
                null,
                "claim-token-1",
                LocalDateTime.now()
        );

        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(10);

        // when
        int updatedCount = notificationRetryRepository.markRetry(
                notificationRetry.getId(),
                "일시적인 발송 오류",
                nextRetryAt,
                "wrong-token"
        );

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PROCESSING);
        assertThat(result.getRetryCount()).isEqualTo(2);
        assertThat(result.getReason()).isNull();
        assertThat(result.getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("재시도 시간이 지난 PENDING 상태의 알림을 PROCESSING 상태로 선점한다")
    void markProcessing() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PENDING,
                1,
                LocalDateTime.now().minusMinutes(5),
                "이전 실패 사유",
                null,
                LocalDateTime.now().minusMinutes(5)
        );

        // when
        int updatedCount = notificationRetryRepository.markProcessing(
                notificationRetry.getId(),
                "claim-token-1",
                LocalDateTime.now().minusMinutes(10)
        );

        entityManager.clear();

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PROCESSING);
        assertThat(result.getClaimToken()).isEqualTo("claim-token-1");
        assertThat(result.getReason()).isNull();
        assertThat(result.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("재시도 시간이 지나지 않은 PENDING 상태의 알림은 선점하지 않는다")
    void markProcessingWhenNextRetryAtIsFuture() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PENDING,
                1,
                LocalDateTime.now().plusMinutes(10),
                "재시도 대기 중",
                null,
                LocalDateTime.now()
        );

        // when
        int updatedCount = notificationRetryRepository.markProcessing(
                notificationRetry.getId(),
                "claim-token-1",
                LocalDateTime.now().minusMinutes(10)
        );

        entityManager.clear();

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PENDING);
        assertThat(result.getClaimToken()).isNull();
        assertThat(result.getReason()).isEqualTo("재시도 대기 중");
    }

    @Test
    @DisplayName("재시도 시간이 NULL인 PENDING 상태의 알림을 PROCESSING 상태로 선점한다")
    void markProcessingWhenNextRetryAtIsNull() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PENDING,
                0,
                null,
                "이전 실패 사유",
                null,
                LocalDateTime.now()
        );

        // when
        int updatedCount = notificationRetryRepository.markProcessing(
                notificationRetry.getId(),
                "claim-token-1",
                LocalDateTime.now().minusMinutes(10)
        );

        entityManager.clear();

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PROCESSING);
        assertThat(result.getClaimToken()).isEqualTo("claim-token-1");
        assertThat(result.getReason()).isNull();
        assertThat(result.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("timeout이 지난 PROCESSING 상태의 알림을 다시 선점한다")
    void markProcessingWhenTimeoutExpired() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        LocalDateTime timeout = LocalDateTime.now().minusMinutes(10);

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PROCESSING,
                1,
                null,
                null,
                "old-claim-token",
                timeout.minusMinutes(5)
        );

        // when
        int updatedCount = notificationRetryRepository.markProcessing(
                notificationRetry.getId(),
                "new-claim-token",
                timeout
        );

        entityManager.clear();

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PROCESSING);
        assertThat(result.getClaimToken()).isEqualTo("new-claim-token");
        assertThat(result.getReason()).isNull();
        assertThat(result.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("timeout이 지나지 않은 PROCESSING 상태의 알림은 다시 선점하지 않는다")
    void markProcessingWhenTimeoutNotExpired() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        LocalDateTime timeout = LocalDateTime.now().minusMinutes(10);

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.PROCESSING,
                1,
                null,
                null,
                "old-claim-token",
                timeout.plusMinutes(5)
        );

        // when
        int updatedCount = notificationRetryRepository.markProcessing(
                notificationRetry.getId(),
                "new-claim-token",
                timeout
        );

        entityManager.clear();

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PROCESSING);
        assertThat(result.getClaimToken()).isEqualTo("old-claim-token");
    }

    @Test
    @DisplayName("PENDING과 timeout이 지난 PROCESSING 상태의 재시도 대상을 조회한다")
    void findRetryTargets() {
        // given
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(10);

        NotificationRetry pending = createNotificationRetry("event-1", 100L, 1L);
        NotificationRetry processing = createNotificationRetry("event-2", 101L, 2L);
        NotificationRetry futurePending = createNotificationRetry("event-3", 102L, 3L);
        NotificationRetry activeProcessing = createNotificationRetry("event-4", 103L, 4L);
        NotificationRetry success = createNotificationRetry("event-5", 104L, 5L);
        NotificationRetry failed = createNotificationRetry("event-6", 105L, 6L);

        persist(
                pending,
                processing,
                futurePending,
                activeProcessing,
                success,
                failed
        );
        flushAndClear();

        updateNotificationRetry(
                pending.getId(),
                NotificationRetryStatus.PENDING,
                1,
                LocalDateTime.now().minusMinutes(5),
                null,
                null,
                LocalDateTime.now()
        );

        updateNotificationRetry(
                processing.getId(),
                NotificationRetryStatus.PROCESSING,
                2,
                null,
                null,
                "claim-token-1",
                timeout.minusMinutes(5)
        );

        updateNotificationRetry(
                futurePending.getId(),
                NotificationRetryStatus.PENDING,
                1,
                LocalDateTime.now().plusMinutes(10),
                null,
                null,
                LocalDateTime.now()
        );

        updateNotificationRetry(
                activeProcessing.getId(),
                NotificationRetryStatus.PROCESSING,
                1,
                null,
                null,
                "claim-token-2",
                timeout.plusMinutes(5)
        );

        updateNotificationRetry(
                processing.getId(),
                NotificationRetryStatus.PROCESSING,
                2,
                LocalDateTime.now().minusMinutes(1),
                null,
                "claim-token-1",
                timeout.minusMinutes(5)
        );

        updateNotificationRetry(
                failed.getId(),
                NotificationRetryStatus.FAILED,
                3,
                null,
                "발송 실패",
                null,
                LocalDateTime.now()
        );

        // when
        Page<NotificationRetry> result = notificationRetryRepository.findRetryTargets(
                timeout,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent())
                .extracting(NotificationRetry::getId)
                .containsExactly(pending.getId(), processing.getId());

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("재시도 대상 조회 시 nextRetryAt, retryCount, ID 순서로 정렬한다")
    void findRetryTargetsWithSorting() {
        // given
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(10);
        LocalDateTime firstRetryAt = LocalDateTime.now().minusMinutes(30);
        LocalDateTime secondRetryAt = LocalDateTime.now().minusMinutes(20);

        NotificationRetry first = createNotificationRetry("event-1", 100L, 1L);
        NotificationRetry second = createNotificationRetry("event-2", 101L, 2L);
        NotificationRetry third = createNotificationRetry("event-3", 102L, 3L);
        NotificationRetry fourth = createNotificationRetry("event-4", 103L, 4L);

        persist(first, second, third, fourth);
        flushAndClear();

        updateNotificationRetry(
                first.getId(),
                NotificationRetryStatus.PENDING,
                1,
                firstRetryAt,
                null,
                null,
                LocalDateTime.now()
        );

        updateNotificationRetry(
                second.getId(),
                NotificationRetryStatus.PENDING,
                2,
                firstRetryAt,
                null,
                null,
                LocalDateTime.now()
        );

        updateNotificationRetry(
                third.getId(),
                NotificationRetryStatus.PENDING,
                1,
                firstRetryAt,
                null,
                null,
                LocalDateTime.now()
        );

        updateNotificationRetry(
                fourth.getId(),
                NotificationRetryStatus.PENDING,
                0,
                secondRetryAt,
                null,
                null,
                LocalDateTime.now()
        );

        // when
        Page<NotificationRetry> result = notificationRetryRepository.findRetryTargets(
                timeout,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent())
                .extracting(NotificationRetry::getId)
                .containsExactly(
                        first.getId(),
                        third.getId(),
                        second.getId(),
                        fourth.getId()
                );
    }

    @Test
    @DisplayName("재시도 대상 조회 시 페이지 크기만큼 조회한다")
    void findRetryTargetsWithPaging() {
        // given
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(10);
        LocalDateTime nextRetryAt = LocalDateTime.now().minusMinutes(5);

        NotificationRetry first = createNotificationRetry("event-1", 100L, 1L);
        NotificationRetry second = createNotificationRetry("event-2", 101L, 2L);
        NotificationRetry third = createNotificationRetry("event-3", 102L, 3L);

        persist(first, second, third);
        flushAndClear();

        updateNotificationRetry(
                first.getId(),
                NotificationRetryStatus.PENDING,
                0,
                nextRetryAt,
                null,
                null,
                LocalDateTime.now()
        );

        updateNotificationRetry(
                second.getId(),
                NotificationRetryStatus.PENDING,
                0,
                nextRetryAt,
                null,
                null,
                LocalDateTime.now()
        );

        updateNotificationRetry(
                third.getId(),
                NotificationRetryStatus.PENDING,
                0,
                nextRetryAt,
                null,
                null,
                LocalDateTime.now()
        );

        // when
        Page<NotificationRetry> result = notificationRetryRepository.findRetryTargets(
                timeout,
                PageRequest.of(0, 2)
        );

        // then
        assertThat(result.getContent())
                .hasSize(2)
                .extracting(NotificationRetry::getId)
                .containsExactly(first.getId(), second.getId());

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("PENDING 상태의 알림 재시도 개수를 조회한다")
    void countPending() {
        // given
        NotificationRetry pending1 = createNotificationRetry("event-1", 100L, 1L);
        NotificationRetry pending2 = createNotificationRetry("event-2", 101L, 2L);
        NotificationRetry processing = createNotificationRetry("event-3", 102L, 3L);
        NotificationRetry success = createNotificationRetry("event-4", 103L, 4L);
        NotificationRetry failed = createNotificationRetry("event-5", 104L, 5L);

        persist(pending1, pending2, processing, success, failed);
        flushAndClear();

        updateNotificationRetry(
                processing.getId(),
                NotificationRetryStatus.PROCESSING,
                0,
                null,
                null,
                "claim-token",
                LocalDateTime.now()
        );

        updateNotificationRetry(
                success.getId(),
                NotificationRetryStatus.SUCCESS,
                0,
                null,
                null,
                null,
                LocalDateTime.now()
        );

        updateNotificationRetry(
                failed.getId(),
                NotificationRetryStatus.FAILED,
                1,
                null,
                "발송 실패",
                null,
                LocalDateTime.now()
        );

        // when
        long result = notificationRetryRepository.countPending();

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("Outbox ID와 Receiver ID로 알림 재시도 이력을 조회한다")
    void findByOutboxIdAndReceiverId() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        // when
        Optional<NotificationRetry> result =
                notificationRetryRepository.findByOutboxIdAndReceiverId(100L, 1L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOutboxId()).isEqualTo(100L);
        assertThat(result.get().getReceiverId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Receiver ID가 일치하지 않으면 알림 재시도 이력을 조회하지 않는다")
    void findByOutboxIdAndReceiverIdWhenReceiverIsDifferent() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        // when
        Optional<NotificationRetry> result =
                notificationRetryRepository.findByOutboxIdAndReceiverId(100L, 2L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("FAILED 상태의 알림 재시도 이력을 복구한다")
    void updateRecover() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        updateNotificationRetry(
                notificationRetry.getId(),
                NotificationRetryStatus.FAILED,
                5,
                LocalDateTime.now().plusDays(1),
                "최대 재시도 횟수 초과",
                "old-claim-token",
                LocalDateTime.now()
        );

        // when
        int updatedCount = notificationRetryRepository.updateRecover(
                notificationRetry.getId()
        );

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PENDING);
        assertThat(result.getRetryCount()).isZero();
        assertThat(result.getNextRetryAt()).isNotNull();
        assertThat(result.getNextRetryAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(result.getReason()).isNull();
        assertThat(result.getClaimToken()).isNull();
    }

    @Test
    @DisplayName("FAILED 상태가 아니면 알림 재시도 이력을 복구하지 않는다")
    void updateRecoverWhenStatusIsNotFailed() {
        // given
        NotificationRetry notificationRetry =
                createNotificationRetry("event-1", 100L, 1L);

        persist(notificationRetry);
        flushAndClear();

        // when
        int updatedCount = notificationRetryRepository.updateRecover(
                notificationRetry.getId()
        );

        entityManager.clear();

        // then
        NotificationRetry result = notificationRetryRepository
                .findById(notificationRetry.getId())
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getNotificationRetryStatus())
                .isEqualTo(NotificationRetryStatus.PENDING);
        assertThat(result.getRetryCount()).isZero();
    }
}