package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.outbox.entity.ProcessedOutbox;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.ProcessedOutboxStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ProcessedOutboxRepositoryTest {

    @Autowired
    private ProcessedOutboxRepository processedOutboxRepository;

    @Autowired
    private EntityManager entityManager;

    private ProcessedOutbox createProcessedOutbox(Long outboxId) {
        return ProcessedOutbox.of(
                outboxId,
                OutboxAggregateType.SCHEDULE,
                OutboxEventType.SCHEDULE_UPDATED
        );
    }

    private void persist(ProcessedOutbox... processedOutboxes) {
        for (ProcessedOutbox processedOutbox : processedOutboxes) {
            entityManager.persist(processedOutbox);
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Outbox ID로 처리 이력을 조회한다")
    void findByOutboxId() {
        // given
        ProcessedOutbox processedOutbox = createProcessedOutbox(100L);

        persist(processedOutbox);
        flushAndClear();

        // when
        Optional<ProcessedOutbox> result =
                processedOutboxRepository.findByOutboxId(100L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOutboxId()).isEqualTo(100L);
        assertThat(result.get().getAggregateType())
                .isEqualTo(OutboxAggregateType.SCHEDULE);
        assertThat(result.get().getEventType())
                .isEqualTo(OutboxEventType.SCHEDULE_UPDATED);
        assertThat(result.get().getStatus())
                .isEqualTo(ProcessedOutboxStatus.PROCESSING);
    }

    @Test
    @DisplayName("존재하지 않는 Outbox ID로 조회하면 빈 Optional을 반환한다")
    void findByOutboxIdWhenNotExists() {
        // given
        ProcessedOutbox processedOutbox = createProcessedOutbox(100L);

        persist(processedOutbox);
        flushAndClear();

        // when
        Optional<ProcessedOutbox> result =
                processedOutboxRepository.findByOutboxId(999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("PROCESSING 상태의 처리 이력을 SUCCESS 상태로 변경한다")
    void markSuccess() {
        // given
        ProcessedOutbox processedOutbox = createProcessedOutbox(100L);

        persist(processedOutbox);
        flushAndClear();

        // when
        int updatedCount = processedOutboxRepository.markSuccess(100L);

        entityManager.clear();

        // then
        ProcessedOutbox result = processedOutboxRepository
                .findByOutboxId(100L)
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getStatus())
                .isEqualTo(ProcessedOutboxStatus.SUCCESS);
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 SUCCESS 상태로 변경하지 않는다")
    void markSuccessWhenStatusIsNotProcessing() {
        // given
        ProcessedOutbox processedOutbox = createProcessedOutbox(100L);

        persist(processedOutbox);
        flushAndClear();

        processedOutboxRepository.markSuccess(100L);
        entityManager.clear();

        // when
        int updatedCount = processedOutboxRepository.markSuccess(100L);

        entityManager.clear();

        // then
        ProcessedOutbox result = processedOutboxRepository
                .findByOutboxId(100L)
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getStatus())
                .isEqualTo(ProcessedOutboxStatus.SUCCESS);
    }

    @Test
    @DisplayName("PROCESSING 상태의 처리 이력을 FAILED 상태로 변경한다")
    void markFailed() {
        // given
        ProcessedOutbox processedOutbox = createProcessedOutbox(100L);

        persist(processedOutbox);
        flushAndClear();

        // when
        int updatedCount = processedOutboxRepository.markFailed(100L);

        entityManager.clear();

        // then
        ProcessedOutbox result = processedOutboxRepository
                .findByOutboxId(100L)
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getStatus())
                .isEqualTo(ProcessedOutboxStatus.FAILED);
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 FAILED 상태로 변경하지 않는다")
    void markFailedWhenStatusIsNotProcessing() {
        // given
        ProcessedOutbox processedOutbox = createProcessedOutbox(100L);

        persist(processedOutbox);
        flushAndClear();

        processedOutboxRepository.markSuccess(100L);
        entityManager.clear();

        // when
        int updatedCount = processedOutboxRepository.markFailed(100L);

        entityManager.clear();

        // then
        ProcessedOutbox result = processedOutboxRepository
                .findByOutboxId(100L)
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getStatus())
                .isEqualTo(ProcessedOutboxStatus.SUCCESS);
    }

    @Test
    @DisplayName("FAILED 상태의 처리 이력을 PROCESSING 상태로 재시도한다")
    void retry() {
        // given
        ProcessedOutbox processedOutbox = createProcessedOutbox(100L);

        persist(processedOutbox);
        flushAndClear();

        processedOutboxRepository.markFailed(100L);
        entityManager.clear();

        // when
        int updatedCount = processedOutboxRepository.retry(100L);

        entityManager.clear();

        // then
        ProcessedOutbox result = processedOutboxRepository
                .findByOutboxId(100L)
                .orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(result.getStatus())
                .isEqualTo(ProcessedOutboxStatus.PROCESSING);
    }

    @Test
    @DisplayName("FAILED 상태가 아니면 PROCESSING 상태로 재시도하지 않는다")
    void retryWhenStatusIsNotFailed() {
        // given
        ProcessedOutbox processedOutbox = createProcessedOutbox(100L);

        persist(processedOutbox);
        flushAndClear();

        // when
        int updatedCount = processedOutboxRepository.retry(100L);

        entityManager.clear();

        // then
        ProcessedOutbox result = processedOutboxRepository
                .findByOutboxId(100L)
                .orElseThrow();

        assertThat(updatedCount).isZero();
        assertThat(result.getStatus())
                .isEqualTo(ProcessedOutboxStatus.PROCESSING);
    }
}