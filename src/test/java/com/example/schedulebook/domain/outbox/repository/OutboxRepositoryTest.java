package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class OutboxRepositoryTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EntityManager entityManager;

    private Outbox createOutbox(String eventId) {
        return Outbox.create(
                eventId,
                OutboxAggregateType.SCHEDULE,
                "100",
                OutboxEventType.SCHEDULE_UPDATED,
                "{\"scheduleId\":100}"
        );
    }

    private Outbox createOutbox(
            String eventId,
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType
    ) {
        return Outbox.create(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                "{\"scheduleId\":100}"
        );
    }

    private void persist(Outbox... outboxes) {
        for (Outbox outbox : outboxes) {
            entityManager.persist(outbox);
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 테스트 데이터의 상태와 재시도 관련 필드를 직접 설정한다.
     *
     * Outbox 엔티티의 상태 변경 메서드가 아닌 Repository 쿼리 자체를
     * 독립적으로 검증하기 위한 테스트 데이터 준비용 메서드다.
     */
    private void updateOutbox(
            Long id,
            OutboxStatus status,
            int retryCount,
            LocalDateTime nextRetryAt,
            LocalDateTime publishedAt,
            String errorMessage,
            LocalDateTime processingAt
    ) {
        entityManager.createNativeQuery("""
                UPDATE outbox
                SET status = :status,
                    retry_count = :retryCount,
                    next_retry_at = :nextRetryAt,
                    published_at = :publishedAt,
                    error_message = :errorMessage,
                    processing_at = :processingAt
                WHERE id = :id
                """)
                .setParameter("status", status.name())
                .setParameter("retryCount", retryCount)
                .setParameter("nextRetryAt", nextRetryAt)
                .setParameter("publishedAt", publishedAt)
                .setParameter("errorMessage", errorMessage)
                .setParameter("processingAt", processingAt)
                .setParameter("id", id)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("재시도 대상 조회 시 PENDING과 재시도 시간이 지난 FAILED를 조회한다")
    void findRetryTargets() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 1, 10, 12, 0);
        LocalDateTime past = now.minusMinutes(10);
        LocalDateTime future = now.plusMinutes(10);

        Outbox pending = createOutbox("event-1");
        Outbox retryableFailed = createOutbox("event-2");
        Outbox waitingFailed = createOutbox("event-3");
        Outbox processing = createOutbox("event-4");
        Outbox success = createOutbox("event-5");

        persist(pending, retryableFailed, waitingFailed, processing, success);
        flushAndClear();

        updateOutbox(
                pending.getId(),
                OutboxStatus.PENDING,
                0,
                future,
                null,
                null,
                null
        );

        updateOutbox(
                retryableFailed.getId(),
                OutboxStatus.FAILED,
                1,
                past,
                null,
                "일시적인 오류",
                null
        );

        updateOutbox(
                waitingFailed.getId(),
                OutboxStatus.FAILED,
                1,
                future,
                null,
                "재시도 대기 중",
                null
        );

        updateOutbox(
                processing.getId(),
                OutboxStatus.PROCESSING,
                0,
                null,
                null,
                null,
                now
        );

        updateOutbox(
                success.getId(),
                OutboxStatus.SUCCESS,
                0,
                null,
                now,
                null,
                null
        );

        // when
        List<Outbox> result = outboxRepository.findRetryTargets(now, 10);

        // then
        assertThat(result)
                .extracting(Outbox::getId)
                .containsExactly(pending.getId(), retryableFailed.getId());
    }

    @Test
    @DisplayName("재시도 대상 조회 시 batchSize만큼만 조회한다")
    void findRetryTargetsWithBatchSize() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 1, 10, 12, 0);

        Outbox first = createOutbox("event-1");
        Outbox second = createOutbox("event-2");
        Outbox third = createOutbox("event-3");

        persist(first, second, third);
        flushAndClear();

        // when
        List<Outbox> result = outboxRepository.findRetryTargets(now, 2);

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Outbox::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("처리 시간이 만료된 PROCESSING 상태의 Outbox를 조회한다")
    void findStuckOutboxes() {
        // given
        LocalDateTime expired = LocalDateTime.of(2026, 1, 10, 12, 0);

        LocalDateTime past = expired.minusMinutes(10);
        LocalDateTime future = expired.plusMinutes(10);

        Outbox stuck = createOutbox("event-1");
        Outbox notStuck = createOutbox("event-2");
        Outbox pending = createOutbox("event-3");
        Outbox failed = createOutbox("event-4");

        persist(stuck, notStuck, pending, failed);
        flushAndClear();

        updateOutbox(
                stuck.getId(),
                OutboxStatus.PROCESSING,
                0,
                null,
                null,
                null,
                past
        );

        updateOutbox(
                notStuck.getId(),
                OutboxStatus.PROCESSING,
                0,
                null,
                null,
                null,
                future
        );

        updateOutbox(
                pending.getId(),
                OutboxStatus.PENDING,
                0,
                null,
                null,
                null,
                past
        );

        updateOutbox(
                failed.getId(),
                OutboxStatus.FAILED,
                1,
                past,
                null,
                "실패",
                past
        );

        // when
        List<Outbox> result = outboxRepository.findStuckOutboxes(expired, 10);

        // then
        assertThat(result)
                .extracting(Outbox::getId)
                .containsExactly(stuck.getId());
    }

    @Test
    @DisplayName("처리 시간이 만료된 Outbox를 batchSize만큼만 조회한다")
    void findStuckOutboxesWithBatchSize() {
        // given
        LocalDateTime expired = LocalDateTime.of(2026, 1, 10, 12, 0);
        LocalDateTime past = expired.minusMinutes(10);

        Outbox first = createOutbox("event-1");
        Outbox second = createOutbox("event-2");
        Outbox third = createOutbox("event-3");

        persist(first, second, third);
        flushAndClear();

        updateOutbox(first.getId(), OutboxStatus.PROCESSING, 0, null, null, null, past);
        updateOutbox(second.getId(), OutboxStatus.PROCESSING, 0, null, null, null, past);
        updateOutbox(third.getId(), OutboxStatus.PROCESSING, 0, null, null, null, past);

        // when
        List<Outbox> result = outboxRepository.findStuckOutboxes(expired, 2);

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Outbox::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("PROCESSING 상태인 Outbox의 발행 성공 상태를 변경한다")
    void updateStatusIfProcessing() {
        // given
        Outbox outbox = createOutbox("event-1");
        persist(outbox);
        flushAndClear();

        LocalDateTime processingAt = LocalDateTime.now()
                .minusMinutes(5)
                .withNano(0);

        LocalDateTime publishedAt = LocalDateTime.now()
                .withNano(0);

        updateOutbox(
                outbox.getId(),
                OutboxStatus.PROCESSING,
                0,
                null,
                null,
                "이전 오류",
                processingAt
        );

        // when
        int updatedCount = outboxRepository.updateStatusIfProcessing(
                outbox.getId(),
                OutboxStatus.SUCCESS,
                publishedAt
        );

        entityManager.clear();

        // then
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.SUCCESS);
        assertThat(result.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(result.getProcessingAt()).isNull();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 발행 성공 상태로 변경하지 않는다")
    void updateStatusIfProcessingWhenStatusIsNotProcessing() {
        // given
        Outbox outbox = createOutbox("event-1");
        persist(outbox);
        flushAndClear();

        LocalDateTime publishedAt = LocalDateTime.now();

        // when
        int updatedCount = outboxRepository.updateStatusIfProcessing(
                outbox.getId(),
                OutboxStatus.SUCCESS,
                publishedAt
        );

        entityManager.clear();

        // then
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태인 Outbox의 실패 정보를 갱신한다")
    void updateFailureIfProcessing() {
        // given
        Outbox outbox = createOutbox("event-1");
        persist(outbox);
        flushAndClear();

        LocalDateTime processingAt = LocalDateTime.now()
                .minusMinutes(5)
                .withNano(0);

        LocalDateTime nextRetryAt = LocalDateTime.now()
                .plusMinutes(5)
                .withNano(0);

        updateOutbox(
                outbox.getId(),
                OutboxStatus.PROCESSING,
                2,
                null,
                null,
                null,
                processingAt
        );

        // when
        int updatedCount = outboxRepository.updateFailureIfProcessing(
                outbox.getId(),
                OutboxStatus.FAILED,
                "메시지 발행 실패",
                nextRetryAt
        );

        entityManager.clear();

        // then
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("메시지 발행 실패");
        assertThat(result.getNextRetryAt()).isEqualTo(nextRetryAt);
        assertThat(result.getRetryCount()).isEqualTo(3);
        assertThat(result.getProcessingAt()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 실패 정보를 갱신하지 않는다")
    void updateFailureIfProcessingWhenStatusIsNotProcessing() {
        // given
        Outbox outbox = createOutbox("event-1");
        persist(outbox);
        flushAndClear();

        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(5);

        // when
        int updatedCount = outboxRepository.updateFailureIfProcessing(
                outbox.getId(),
                OutboxStatus.FAILED,
                "메시지 발행 실패",
                nextRetryAt
        );

        entityManager.clear();

        // then
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getRetryCount()).isZero();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태인 Outbox의 복구 실패 정보를 갱신한다")
    void updateRecoverIfProcessing() {
        // given
        Outbox outbox = createOutbox("event-1");
        persist(outbox);
        flushAndClear();

        LocalDateTime processingAt = LocalDateTime.now()
                .minusMinutes(5)
                .withNano(0);

        LocalDateTime nextRetryAt = LocalDateTime.now()
                .plusMinutes(10)
                .withNano(0);

        updateOutbox(
                outbox.getId(),
                OutboxStatus.PROCESSING,
                3,
                null,
                null,
                null,
                processingAt
        );

        // when
        int updatedCount = outboxRepository.updateRecoverIfProcessing(
                outbox.getId(),
                OutboxStatus.FAILED,
                "처리 중단된 Outbox 복구 실패",
                nextRetryAt
        );

        entityManager.clear();

        // then
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("처리 중단된 Outbox 복구 실패");
        assertThat(result.getNextRetryAt()).isEqualTo(nextRetryAt);
        assertThat(result.getRetryCount()).isEqualTo(4);
        assertThat(result.getProcessingAt()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 복구 실패 정보를 갱신하지 않는다")
    void updateRecoverIfProcessingWhenStatusIsNotProcessing() {
        // given
        Outbox outbox = createOutbox("event-1");
        persist(outbox);
        flushAndClear();

        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(10);

        // when
        int updatedCount = outboxRepository.updateRecoverIfProcessing(
                outbox.getId(),
                OutboxStatus.FAILED,
                "복구 실패",
                nextRetryAt
        );

        entityManager.clear();

        // then
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getRetryCount()).isZero();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("특정 상태의 Outbox를 페이징 조회한다")
    void findAllByStatus() {
        // given
        Outbox pending1 = createOutbox("event-1");
        Outbox pending2 = createOutbox("event-2");
        Outbox pending3 = createOutbox("event-3");
        Outbox failed = createOutbox("event-4");

        persist(pending1, pending2, pending3, failed);
        flushAndClear();

        updateOutbox(
                failed.getId(),
                OutboxStatus.FAILED,
                1,
                LocalDateTime.now().plusMinutes(5),
                null,
                "실패",
                null
        );

        // when
        Page<Outbox> result = outboxRepository.findAllByStatus(
                OutboxStatus.PENDING,
                PageRequest.of(0, 2)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);

        assertThat(result.getContent())
                .extracting(Outbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("발행된 지 일정 시간이 지난 SUCCESS 상태의 Outbox를 삭제한다")
    void deleteSuccessBefore() {
        // given
        LocalDateTime target = LocalDateTime.of(2026, 1, 10, 12, 0);

        Outbox oldSuccess = createOutbox("event-1");
        Outbox recentSuccess = createOutbox("event-2");
        Outbox failed = createOutbox("event-3");

        persist(oldSuccess, recentSuccess, failed);
        flushAndClear();

        updateOutbox(
                oldSuccess.getId(),
                OutboxStatus.SUCCESS,
                0,
                null,
                target.minusDays(1),
                null,
                null
        );

        updateOutbox(
                recentSuccess.getId(),
                OutboxStatus.SUCCESS,
                0,
                null,
                target.plusDays(1),
                null,
                null
        );

        updateOutbox(
                failed.getId(),
                OutboxStatus.FAILED,
                1,
                null,
                target.minusDays(1),
                "실패",
                null
        );

        // when
        int deletedCount = outboxRepository.deleteSuccessBefore(target);

        entityManager.clear();

        // then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(outboxRepository.findById(oldSuccess.getId())).isEmpty();
        assertThat(outboxRepository.findById(recentSuccess.getId())).isPresent();
        assertThat(outboxRepository.findById(failed.getId())).isPresent();
    }

    @Test
    @DisplayName("Outbox 상태별 통계를 조회한다")
    void countStats() {
        // given
        Outbox pending = createOutbox("event-1");
        Outbox processing = createOutbox("event-2");
        Outbox failed = createOutbox("event-3");
        Outbox dead = createOutbox("event-4");
        Outbox success = createOutbox("event-5");
        Outbox anotherPending = createOutbox("event-6");

        persist(pending, processing, failed, dead, success, anotherPending);
        flushAndClear();

        updateOutbox(processing.getId(), OutboxStatus.PROCESSING, 0, null, null, null, LocalDateTime.now());
        updateOutbox(failed.getId(), OutboxStatus.FAILED, 1, LocalDateTime.now(), null, "실패", null);
        updateOutbox(dead.getId(), OutboxStatus.DEAD, 5, null, null, "최대 재시도 초과", null);
        updateOutbox(success.getId(), OutboxStatus.SUCCESS, 0, null, LocalDateTime.now(), null, null);

        // when
        Object[] result = (Object[]) outboxRepository.countStats()[0];

        // then
        assertThat(result).hasSize(5);

        assertThat(((Number) result[0]).longValue()).isEqualTo(2); // PENDING
        assertThat(((Number) result[1]).longValue()).isEqualTo(1); // PROCESSING
        assertThat(((Number) result[2]).longValue()).isEqualTo(1); // FAILED
        assertThat(((Number) result[3]).longValue()).isEqualTo(1); // DEAD
        assertThat(((Number) result[4]).longValue()).isEqualTo(1); // SUCCESS
    }

    @Test
    @DisplayName("지정한 Aggregate와 Event의 PENDING Outbox를 DEAD 상태로 변경한다")
    void updateStatusToDeadIfPending() {
        // given
        Outbox target = createOutbox("event-1");

        Outbox anotherAggregate = createOutbox(
                "event-2",
                OutboxAggregateType.USER,
                "100",
                OutboxEventType.SCHEDULE_UPDATED
        );

        Outbox anotherEvent = createOutbox(
                "event-3",
                OutboxAggregateType.SCHEDULE,
                "100",
                OutboxEventType.SCHEDULE_CANCELED
        );

        Outbox processing = createOutbox("event-4");

        persist(target, anotherAggregate, anotherEvent, processing);
        flushAndClear();

        updateOutbox(
                processing.getId(),
                OutboxStatus.PROCESSING,
                0,
                null,
                null,
                null,
                LocalDateTime.now()
        );

        // when
        outboxRepository.updateStatusToDeadIfPending(
                OutboxAggregateType.SCHEDULE,
                "100",
                OutboxEventType.SCHEDULE_UPDATED
        );

        entityManager.clear();

        // then
        Outbox deadOutbox = outboxRepository.findById(target.getId()).orElseThrow();
        Outbox otherAggregateOutbox = outboxRepository.findById(anotherAggregate.getId()).orElseThrow();
        Outbox otherEventOutbox = outboxRepository.findById(anotherEvent.getId()).orElseThrow();
        Outbox processingOutbox = outboxRepository.findById(processing.getId()).orElseThrow();

        assertThat(deadOutbox.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(deadOutbox.getNextRetryAt()).isNull();

        assertThat(otherAggregateOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(otherEventOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(processingOutbox.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
    }

    @Test
    @DisplayName("DEAD 상태의 Outbox를 PENDING 상태로 복구한다")
    void updateRecover() {
        // given
        Outbox outbox = createOutbox("event-1");
        persist(outbox);
        flushAndClear();

        LocalDateTime publishedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime processingAt = LocalDateTime.now().minusMinutes(10);

        updateOutbox(
                outbox.getId(),
                OutboxStatus.DEAD,
                5,
                LocalDateTime.now().plusDays(1),
                publishedAt,
                "최대 재시도 횟수 초과",
                processingAt
        );

        // when
        int updatedCount = outboxRepository.updateRecover(outbox.getId());

        entityManager.clear();

        // then
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getRetryCount()).isZero();
        assertThat(result.getNextRetryAt()).isNull();
        assertThat(result.getProcessingAt()).isNull();
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("DEAD 상태가 아니면 Outbox를 복구하지 않는다")
    void updateRecoverWhenStatusIsNotDead() {
        // given
        Outbox outbox = createOutbox("event-1");
        persist(outbox);
        flushAndClear();

        // when
        int updatedCount = outboxRepository.updateRecover(outbox.getId());

        entityManager.clear();

        // then
        Outbox result = outboxRepository.findById(outbox.getId()).orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getRetryCount()).isZero();
    }
}