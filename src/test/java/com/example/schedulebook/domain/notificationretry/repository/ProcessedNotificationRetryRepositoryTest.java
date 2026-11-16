package com.example.schedulebook.domain.notificationretry.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.notificationretry.entity.ProcessedNotificationRetry;
import com.example.schedulebook.domain.notificationretry.enums.ProcessedNotificationRetryStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ProcessedNotificationRetryRepositoryTest {

    @Autowired
    private ProcessedNotificationRetryRepository processedNotificationRetryRepository;

    @Autowired
    private EntityManager entityManager;

    private ProcessedNotificationRetry createProcessedNotificationRetry(
            Long outboxId,
            Long receiverId,
            String owner
    ) {
        return ProcessedNotificationRetry.create(outboxId, receiverId, owner);
    }

    private void persist(ProcessedNotificationRetry... retries) {
        for (ProcessedNotificationRetry retry : retries) {
            entityManager.persist(retry);
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void updateProcessedNotificationRetry(
            Long id,
            ProcessedNotificationRetryStatus status,
            String owner,
            LocalDateTime updatedAt
    ) {
        entityManager.createNativeQuery("""
                UPDATE processed_notification_retry
                SET status = :status,
                    processing_owner = :owner,
                    updated_at = :updatedAt
                WHERE id = :id
                """)
                .setParameter("status", status.name())
                .setParameter("owner", owner)
                .setParameter("updatedAt", updatedAt)
                .setParameter("id", id)
                .executeUpdate();

        flushAndClear();
    }

    // ---------------------------------------------------------
    // findByOutboxIdAndReceiverId
    // ---------------------------------------------------------

    @Test
    @DisplayName("Outbox ID와 Receiver ID로 처리 이력을 조회한다")
    void findByOutboxIdAndReceiverId() {
        // given
        ProcessedNotificationRetry retry =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        persist(retry);
        flushAndClear();

        // when
        Optional<ProcessedNotificationRetry> result =
                processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                        100L,
                        1L
                );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOutboxId()).isEqualTo(100L);
        assertThat(result.get().getReceiverId()).isEqualTo(1L);
        assertThat(result.get().getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(result.get().getProcessingOwner()).isEqualTo("owner-1");
    }

    @Test
    @DisplayName("존재하지 않는 Outbox ID와 Receiver ID로 조회하면 빈 Optional을 반환한다")
    void findByOutboxIdAndReceiverIdWhenNotExists() {
        // when
        Optional<ProcessedNotificationRetry> result =
                processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                        999L,
                        999L
                );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Receiver ID가 일치하지 않으면 처리 이력을 조회하지 못한다")
    void findByOutboxIdAndReceiverIdWhenReceiverIdIsDifferent() {
        // given
        ProcessedNotificationRetry retry =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        persist(retry);
        flushAndClear();

        // when
        Optional<ProcessedNotificationRetry> result =
                processedNotificationRetryRepository.findByOutboxIdAndReceiverId(
                        100L,
                        2L
                );

        // then
        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------
    // markSuccess
    // ---------------------------------------------------------

    @Test
    @DisplayName("PROCESSING 상태에서 올바른 owner로 SUCCESS 상태 변경 및 owner 초기화")
    void markSuccess() {
        // given
        ProcessedNotificationRetry target =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        ProcessedNotificationRetry differentOutbox =
                createProcessedNotificationRetry(200L, 1L, "owner-1");

        ProcessedNotificationRetry differentReceiver =
                createProcessedNotificationRetry(100L, 2L, "owner-1");

        persist(target, differentOutbox, differentReceiver);
        flushAndClear();

        // when
        int result = processedNotificationRetryRepository.markSuccess(
                100L,
                1L,
                "owner-1"
        );

        // then
        assertThat(result).isEqualTo(1);

        ProcessedNotificationRetry updatedTarget =
                processedNotificationRetryRepository.findById(target.getId())
                        .orElseThrow();

        ProcessedNotificationRetry unchangedOutbox =
                processedNotificationRetryRepository.findById(differentOutbox.getId())
                        .orElseThrow();

        ProcessedNotificationRetry unchangedReceiver =
                processedNotificationRetryRepository.findById(differentReceiver.getId())
                        .orElseThrow();

        // 변경 대상
        assertThat(updatedTarget.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.SUCCESS);
        assertThat(updatedTarget.getProcessingOwner()).isNull();

        // Outbox ID가 다른 데이터
        assertThat(unchangedOutbox.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(unchangedOutbox.getProcessingOwner()).isEqualTo("owner-1");

        // Receiver ID가 다른 데이터
        assertThat(unchangedReceiver.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(unchangedReceiver.getProcessingOwner()).isEqualTo("owner-1");
    }

    @Test
    @DisplayName("owner가 일치하지 않으면 SUCCESS 상태로 변경하지 않는다")
    void markSuccessWhenOwnerIsDifferent() {
        // given
        ProcessedNotificationRetry retry =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        persist(retry);
        flushAndClear();

        // when
        int result = processedNotificationRetryRepository.markSuccess(
                100L,
                1L,
                "owner-2"
        );

        // then
        assertThat(result).isZero();

        ProcessedNotificationRetry found =
                processedNotificationRetryRepository.findById(retry.getId())
                        .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(found.getProcessingOwner()).isEqualTo("owner-1");
    }

    @Test
    @DisplayName("PROCESSING 상태가 아니면 SUCCESS 상태로 변경하지 않는다")
    void markSuccessWhenStatusIsNotProcessing() {
        // given
        ProcessedNotificationRetry retry =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        persist(retry);
        flushAndClear();

        updateProcessedNotificationRetry(
                retry.getId(),
                ProcessedNotificationRetryStatus.FAILED,
                "owner-1",
                LocalDateTime.now()
        );

        // when
        int result = processedNotificationRetryRepository.markSuccess(
                100L,
                1L,
                "owner-1"
        );

        // then
        assertThat(result).isZero();

        ProcessedNotificationRetry found =
                processedNotificationRetryRepository.findById(retry.getId())
                        .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.FAILED);
        assertThat(found.getProcessingOwner()).isEqualTo("owner-1");
    }

    // ---------------------------------------------------------
    // markRetry
    // ---------------------------------------------------------

    @Test
    @DisplayName("FAILED 상태의 처리 이력을 PROCESSING 상태로 변경하고 owner를 설정한다")
    void markRetry() {
        // given
        ProcessedNotificationRetry target =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        ProcessedNotificationRetry differentOutbox =
                createProcessedNotificationRetry(200L, 1L, "owner-1");

        ProcessedNotificationRetry differentReceiver =
                createProcessedNotificationRetry(100L, 2L, "owner-1");

        persist(target, differentOutbox, differentReceiver);
        flushAndClear();

        updateProcessedNotificationRetry(
                target.getId(),
                ProcessedNotificationRetryStatus.FAILED,
                null,
                LocalDateTime.now()
        );

        updateProcessedNotificationRetry(
                differentOutbox.getId(),
                ProcessedNotificationRetryStatus.FAILED,
                null,
                LocalDateTime.now()
        );

        updateProcessedNotificationRetry(
                differentReceiver.getId(),
                ProcessedNotificationRetryStatus.FAILED,
                null,
                LocalDateTime.now()
        );

        // when
        int result = processedNotificationRetryRepository.markRetry(
                100L,
                1L,
                "owner-2"
        );

        // then
        assertThat(result).isEqualTo(1);

        ProcessedNotificationRetry updatedTarget =
                processedNotificationRetryRepository.findById(target.getId())
                        .orElseThrow();

        ProcessedNotificationRetry unchangedOutbox =
                processedNotificationRetryRepository.findById(differentOutbox.getId())
                        .orElseThrow();

        ProcessedNotificationRetry unchangedReceiver =
                processedNotificationRetryRepository.findById(differentReceiver.getId())
                        .orElseThrow();

        // 변경 대상
        assertThat(updatedTarget.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(updatedTarget.getProcessingOwner()).isEqualTo("owner-2");

        // Outbox ID가 다른 데이터
        assertThat(unchangedOutbox.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.FAILED);
        assertThat(unchangedOutbox.getProcessingOwner()).isNull();

        // Receiver ID가 다른 데이터
        assertThat(unchangedReceiver.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.FAILED);
        assertThat(unchangedReceiver.getProcessingOwner()).isNull();
    }

    @Test
    @DisplayName("FAILED 상태가 아니면 PROCESSING 상태로 변경하지 않는다")
    void markRetryWhenStatusIsNotFailed() {
        // given
        ProcessedNotificationRetry retry =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        persist(retry);
        flushAndClear();

        // when
        int result = processedNotificationRetryRepository.markRetry(
                100L,
                1L,
                "owner-2"
        );

        // then
        assertThat(result).isZero();

        ProcessedNotificationRetry found =
                processedNotificationRetryRepository.findById(retry.getId())
                        .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(found.getProcessingOwner()).isEqualTo("owner-1");
    }

    // ---------------------------------------------------------
    // markFailed
    // ---------------------------------------------------------

    @Test
    @DisplayName("PROCESSING 상태에서 올바른 owner로 FAILED 상태 변경 및 owner 초기화")
    void markFailed() {
        // given
        ProcessedNotificationRetry target =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        ProcessedNotificationRetry differentOutbox =
                createProcessedNotificationRetry(200L, 1L, "owner-1");

        ProcessedNotificationRetry differentReceiver =
                createProcessedNotificationRetry(100L, 2L, "owner-1");

        persist(target, differentOutbox, differentReceiver);
        flushAndClear();

        // when
        int result = processedNotificationRetryRepository.markFailed(
                100L,
                1L,
                "owner-1"
        );

        // then
        assertThat(result).isEqualTo(1);

        ProcessedNotificationRetry updatedTarget =
                processedNotificationRetryRepository.findById(target.getId())
                        .orElseThrow();

        ProcessedNotificationRetry unchangedOutbox =
                processedNotificationRetryRepository.findById(differentOutbox.getId())
                        .orElseThrow();

        ProcessedNotificationRetry unchangedReceiver =
                processedNotificationRetryRepository.findById(differentReceiver.getId())
                        .orElseThrow();

        // 변경 대상
        assertThat(updatedTarget.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.FAILED);
        assertThat(updatedTarget.getProcessingOwner()).isNull();

        // Outbox ID가 다른 데이터
        assertThat(unchangedOutbox.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(unchangedOutbox.getProcessingOwner()).isEqualTo("owner-1");

        // Receiver ID가 다른 데이터
        assertThat(unchangedReceiver.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(unchangedReceiver.getProcessingOwner()).isEqualTo("owner-1");
    }

    @Test
    @DisplayName("owner가 일치하지 않으면 FAILED 상태로 변경하지 않는다")
    void markFailedWhenOwnerIsDifferent() {
        // given
        ProcessedNotificationRetry retry =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        persist(retry);
        flushAndClear();

        // when
        int result = processedNotificationRetryRepository.markFailed(
                100L,
                1L,
                "owner-2"
        );

        // then
        assertThat(result).isZero();

        ProcessedNotificationRetry found =
                processedNotificationRetryRepository.findById(retry.getId())
                        .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(found.getProcessingOwner()).isEqualTo("owner-1");
    }

    // ---------------------------------------------------------
    // recoverTimeoutProcessing
    // ---------------------------------------------------------

    @Test
    @DisplayName("Timeout이 지난 PROCESSING 상태의 처리 이력을 FAILED 상태로 복구한다")
    void recoverTimeoutProcessing() {
        // given
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(10);

        ProcessedNotificationRetry expiredRetry1 =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        ProcessedNotificationRetry expiredRetry2 =
                createProcessedNotificationRetry(101L, 2L, "owner-2");

        ProcessedNotificationRetry activeRetry =
                createProcessedNotificationRetry(102L, 3L, "owner-3");

        ProcessedNotificationRetry successRetry =
                createProcessedNotificationRetry(103L, 4L, "owner-4");

        persist(expiredRetry1, expiredRetry2, activeRetry, successRetry);
        flushAndClear();

        updateProcessedNotificationRetry(
                expiredRetry1.getId(),
                ProcessedNotificationRetryStatus.PROCESSING,
                "owner-1",
                timeout.minusMinutes(5)
        );

        updateProcessedNotificationRetry(
                expiredRetry2.getId(),
                ProcessedNotificationRetryStatus.PROCESSING,
                "owner-2",
                timeout.minusMinutes(1)
        );

        updateProcessedNotificationRetry(
                activeRetry.getId(),
                ProcessedNotificationRetryStatus.PROCESSING,
                "owner-3",
                timeout.plusMinutes(5)
        );

        updateProcessedNotificationRetry(
                successRetry.getId(),
                ProcessedNotificationRetryStatus.SUCCESS,
                null,
                timeout.minusMinutes(5)
        );

        // when
        int result = processedNotificationRetryRepository.recoverTimeoutProcessing(timeout);

        // then
        assertThat(result).isEqualTo(2);

        ProcessedNotificationRetry updatedExpired1 =
                processedNotificationRetryRepository.findById(expiredRetry1.getId())
                        .orElseThrow();

        ProcessedNotificationRetry updatedExpired2 =
                processedNotificationRetryRepository.findById(expiredRetry2.getId())
                        .orElseThrow();

        ProcessedNotificationRetry unchangedActive =
                processedNotificationRetryRepository.findById(activeRetry.getId())
                        .orElseThrow();

        ProcessedNotificationRetry unchangedSuccess =
                processedNotificationRetryRepository.findById(successRetry.getId())
                        .orElseThrow();

        assertThat(updatedExpired1.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.FAILED);
        assertThat(updatedExpired1.getProcessingOwner()).isNull();

        assertThat(updatedExpired2.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.FAILED);
        assertThat(updatedExpired2.getProcessingOwner()).isNull();

        assertThat(unchangedActive.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(unchangedActive.getProcessingOwner()).isEqualTo("owner-3");

        assertThat(unchangedSuccess.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.SUCCESS);
        assertThat(unchangedSuccess.getProcessingOwner()).isNull();
    }

    @Test
    @DisplayName("Timeout이 지나지 않은 PROCESSING 상태의 처리 이력은 복구하지 않는다")
    void recoverTimeoutProcessingWhenTimeoutIsNotExpired() {
        // given
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(10);

        ProcessedNotificationRetry retry =
                createProcessedNotificationRetry(100L, 1L, "owner-1");

        persist(retry);
        flushAndClear();

        updateProcessedNotificationRetry(
                retry.getId(),
                ProcessedNotificationRetryStatus.PROCESSING,
                "owner-1",
                timeout.plusMinutes(5)
        );

        // when
        int result = processedNotificationRetryRepository.recoverTimeoutProcessing(timeout);

        // then
        assertThat(result).isZero();

        ProcessedNotificationRetry found =
                processedNotificationRetryRepository.findById(retry.getId())
                        .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(ProcessedNotificationRetryStatus.PROCESSING);
        assertThat(found.getProcessingOwner()).isEqualTo("owner-1");
    }
}