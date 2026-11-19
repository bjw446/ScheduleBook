package com.example.schedulebook.domain.deadletter.repository;

import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@ActiveProfiles("test")
class DeadLetterRepositoryTest {

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @Autowired
    private EntityManager entityManager;

    private static final LocalDateTime FUTURE_LEASE_UNTIL =
            LocalDateTime.of(2099, 1, 15, 12, 0);

    private static final LocalDateTime EXPIRED_LEASE_UNTIL =
            LocalDateTime.of(2000, 1, 15, 12, 0);

    // ==============================
    // markProcessing
    // ==============================

    @Test
    @DisplayName("PENDING 상태의 DeadLetter를 PROCESSING 상태로 선점한다")
    void markProcessingWithPending() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-1");
        String claimToken = "claim-token-1";
        LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(5);

        // when
        int updatedCount = deadLetterRepository.markProcessing(
                deadLetter.getId(),
                claimToken,
                leaseUntil
        );

        // then
        assertThat(updatedCount).isEqualTo(1);

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(claimToken);
        assertThat(found.getLeaseUntil())
                .isCloseTo(leaseUntil, within(1, ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("유효한 lease가 존재하는 PROCESSING 상태의 DeadLetter는 재선점하지 않는다")
    void markProcessingWithActiveLease() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-2");

        String previousToken = "previous-token";
        String newToken = "new-token";

        LocalDateTime previousLeaseUntil = FUTURE_LEASE_UNTIL;

        updateProcessing(deadLetter.getId(), previousToken, previousLeaseUntil);

        LocalDateTime newLeaseUntil = FUTURE_LEASE_UNTIL.plusDays(1);

        // when
        int updatedCount = deadLetterRepository.markProcessing(
                deadLetter.getId(),
                newToken,
                newLeaseUntil
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(previousToken);
        assertThat(found.getLeaseUntil())
                .isCloseTo(previousLeaseUntil, within(1, ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("lease가 만료된 PROCESSING 상태의 DeadLetter를 재선점한다")
    void markProcessingWithExpiredLease() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-3");

        String previousToken = "previous-token";
        String newToken = "new-token";

        LocalDateTime expiredLeaseUntil = EXPIRED_LEASE_UNTIL;
        LocalDateTime newLeaseUntil = FUTURE_LEASE_UNTIL;

        updateProcessing(deadLetter.getId(), previousToken, expiredLeaseUntil);

        // when
        int updatedCount = deadLetterRepository.markProcessing(
                deadLetter.getId(),
                newToken,
                newLeaseUntil
        );

        // then
        assertThat(updatedCount).isEqualTo(1);

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(newToken);
        assertThat(found.getLeaseUntil())
                .isCloseTo(newLeaseUntil, within(1, ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("존재하지 않는 DeadLetter는 PROCESSING 상태로 선점하지 않는다")
    void markProcessingWithNonExistentId() {
        // given
        DeadLetterQueue pending = saveDeadLetter("event-non-existent-1");

        Long nonExistentId = 999L;
        String claimToken = "claim-token";

        LocalDateTime leaseUntil = getDatabaseTime().plusMinutes(5);

        // when
        int updatedCount = deadLetterRepository.markProcessing(
                nonExistentId,
                claimToken,
                leaseUntil
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(pending.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PENDING);
        assertThat(found.getClaimToken()).isNull();
        assertThat(found.getLeaseUntil()).isNull();
    }

    // ==============================
    // markRecovered
    // ==============================

    @Test
    @DisplayName("올바른 claimToken을 가진 PROCESSING 상태의 DeadLetter를 RECOVERED 상태로 변경한다")
    void markRecoveredWithValidClaimToken() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-4");
        String claimToken = "claim-token";

        updateProcessing(
                deadLetter.getId(),
                claimToken,
                LocalDateTime.now().plusMinutes(5)
        );

        // when
        int updatedCount = deadLetterRepository.markRecovered(
                deadLetter.getId(),
                claimToken
        );

        // then
        assertThat(updatedCount).isEqualTo(1);

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.RECOVERED);
        assertThat(found.getClaimToken()).isNull();
        assertThat(found.getLeaseUntil()).isNull();
    }

    @Test
    @DisplayName("잘못된 claimToken으로는 DeadLetter를 RECOVERED 상태로 변경하지 않는다")
    void markRecoveredWithInvalidClaimToken() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-5");
        String claimToken = "correct-token";

        LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(5);

        updateProcessing(deadLetter.getId(), claimToken, leaseUntil);

        // when
        int updatedCount = deadLetterRepository.markRecovered(
                deadLetter.getId(),
                "wrong-token"
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(claimToken);
        assertThat(found.getLeaseUntil())
                .isCloseTo(leaseUntil, within(1, ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("PROCESSING 상태가 아닌 DeadLetter는 RECOVERED 상태로 변경하지 않는다")
    void markRecoveredWithPendingStatus() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-6");

        // when
        int updatedCount = deadLetterRepository.markRecovered(
                deadLetter.getId(),
                "claim-token"
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PENDING);
    }

    @Test
    @DisplayName("존재하지 않는 DeadLetter는 RECOVERED 상태로 변경하지 않는다")
    void markRecoveredWithNonExistentId() {
        // given
        DeadLetterQueue processing = saveDeadLetter("event-non-existent-2");

        String claimToken = "claim-token";

        updateProcessing(
                processing.getId(),
                claimToken,
                getDatabaseTime().plusMinutes(5)
        );

        Long nonExistentId = 999L;

        // when
        int updatedCount = deadLetterRepository.markRecovered(
                nonExistentId,
                claimToken
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(processing.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(claimToken);
    }

    // ==============================
    // markPending
    // ==============================

    @Test
    @DisplayName("올바른 claimToken을 가진 PROCESSING 상태의 DeadLetter를 PENDING 상태로 변경한다")
    void markPendingWithValidClaimToken() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-7");
        String claimToken = "claim-token";

        updateProcessing(
                deadLetter.getId(),
                claimToken,
                LocalDateTime.now().plusMinutes(5)
        );

        // when
        int updatedCount = deadLetterRepository.markPending(
                deadLetter.getId(),
                claimToken
        );

        // then
        assertThat(updatedCount).isEqualTo(1);

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PENDING);
        assertThat(found.getClaimToken()).isNull();
        assertThat(found.getLeaseUntil()).isNull();
    }

    @Test
    @DisplayName("잘못된 claimToken으로는 DeadLetter를 PENDING 상태로 변경하지 않는다")
    void markPendingWithInvalidClaimToken() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-8");
        String claimToken = "correct-token";

        LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(5);

        updateProcessing(deadLetter.getId(), claimToken, leaseUntil);

        // when
        int updatedCount = deadLetterRepository.markPending(
                deadLetter.getId(),
                "wrong-token"
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(claimToken);
        assertThat(found.getLeaseUntil())
                .isCloseTo(leaseUntil, within(1, ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("PROCESSING 상태가 아닌 DeadLetter는 PENDING 상태로 변경하지 않는다")
    void markPendingWithPendingStatus() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-9");

        // when
        int updatedCount = deadLetterRepository.markPending(
                deadLetter.getId(),
                "claim-token"
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PENDING);
    }

    @Test
    @DisplayName("존재하지 않는 DeadLetter는 PENDING 상태로 변경하지 않는다")
    void markPendingWithNonExistentId() {
        // given
        DeadLetterQueue processing = saveDeadLetter("event-non-existent-3");

        String claimToken = "claim-token";

        updateProcessing(
                processing.getId(),
                claimToken,
                getDatabaseTime().plusMinutes(5)
        );

        Long nonExistentId = 999L;

        // when
        int updatedCount = deadLetterRepository.markPending(
                nonExistentId,
                claimToken
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(processing.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(claimToken);
    }

    // ==============================
    // reclaimExpiredProcessing
    // ==============================

    @Test
    @DisplayName("lease가 만료된 PROCESSING 상태의 DeadLetter를 PENDING 상태로 회수한다")
    void reclaimExpiredProcessing() {
        // given
        DeadLetterQueue expired = saveDeadLetter("event-10");
        DeadLetterQueue active = saveDeadLetter("event-11");

        updateProcessing(
                expired.getId(),
                "expired-token",
                EXPIRED_LEASE_UNTIL
        );

        updateProcessing(
                active.getId(),
                "active-token",
                FUTURE_LEASE_UNTIL
        );

        // when
        int updatedCount = deadLetterRepository.reclaimExpiredProcessing();

        // then
        assertThat(updatedCount).isEqualTo(1);

        entityManager.clear();

        DeadLetterQueue foundExpired = findById(expired.getId());
        DeadLetterQueue foundActive = findById(active.getId());

        assertThat(foundExpired.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PENDING);
        assertThat(foundExpired.getClaimToken()).isNull();
        assertThat(foundExpired.getLeaseUntil()).isNull();

        assertThat(foundActive.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(foundActive.getClaimToken()).isEqualTo("active-token");
    }

    @Test
    @DisplayName("만료된 PROCESSING 상태의 DeadLetter가 없으면 회수하지 않는다")
    void reclaimExpiredProcessingWithoutExpiredTarget() {
        // given
        DeadLetterQueue active = saveDeadLetter("event-12");

        updateProcessing(
                active.getId(),
                "active-token",
                FUTURE_LEASE_UNTIL
        );

        // when
        int updatedCount = deadLetterRepository.reclaimExpiredProcessing();

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(active.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo("active-token");
    }

    // ==============================
    // renewLease
    // ==============================

    @Test
    @DisplayName("올바른 claimToken을 가진 PROCESSING 상태의 DeadLetter lease를 갱신한다")
    void renewLeaseWithValidClaimToken() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-13");
        String claimToken = "claim-token";

        LocalDateTime previousLeaseUntil = LocalDateTime.now().plusMinutes(5);
        LocalDateTime newLeaseUntil = LocalDateTime.now().plusMinutes(10);

        updateProcessing(deadLetter.getId(), claimToken, previousLeaseUntil);

        // when
        int updatedCount = deadLetterRepository.renewLease(
                deadLetter.getId(),
                claimToken,
                newLeaseUntil
        );

        // then
        assertThat(updatedCount).isEqualTo(1);

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(claimToken);
        assertThat(found.getLeaseUntil())
                .isCloseTo(newLeaseUntil, within(1, ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("잘못된 claimToken으로는 DeadLetter lease를 갱신하지 않는다")
    void renewLeaseWithInvalidClaimToken() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-14");
        String claimToken = "correct-token";

        LocalDateTime previousLeaseUntil = LocalDateTime.now().plusMinutes(5);

        updateProcessing(deadLetter.getId(), claimToken, previousLeaseUntil);

        // when
        int updatedCount = deadLetterRepository.renewLease(
                deadLetter.getId(),
                "wrong-token",
                LocalDateTime.now().plusMinutes(10)
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getClaimToken()).isEqualTo(claimToken);
        assertThat(found.getLeaseUntil())
                .isCloseTo(previousLeaseUntil, within(1, ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("PROCESSING 상태가 아닌 DeadLetter lease는 갱신하지 않는다")
    void renewLeaseWithPendingStatus() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-15");

        // when
        int updatedCount = deadLetterRepository.renewLease(
                deadLetter.getId(),
                "claim-token",
                LocalDateTime.now().plusMinutes(10)
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(deadLetter.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PENDING);
        assertThat(found.getLeaseUntil()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 DeadLetter lease는 갱신하지 않는다")
    void renewLeaseWithNonExistentId() {
        // given
        DeadLetterQueue processing = saveDeadLetter("event-non-existent-4");

        String claimToken = "claim-token";

        LocalDateTime previousLeaseUntil = FUTURE_LEASE_UNTIL;

        updateProcessing(
                processing.getId(),
                claimToken,
                previousLeaseUntil
        );

        Long nonExistentId = 999L;

        // when
        int updatedCount = deadLetterRepository.renewLease(
                nonExistentId,
                claimToken,
                FUTURE_LEASE_UNTIL.plusDays(1)
        );

        // then
        assertThat(updatedCount).isZero();

        entityManager.clear();

        DeadLetterQueue found = findById(processing.getId());

        assertThat(found.getDeadLetterStatus()).isEqualTo(DeadLetterStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo(claimToken);
        assertThat(found.getLeaseUntil())
                .isCloseTo(previousLeaseUntil, within(1, ChronoUnit.MICROS));
    }

    // ==============================
    // findByIdAndClaimTokenAndDeadLetterStatus
    // ==============================

    @Test
    @DisplayName("ID, claimToken, 상태가 일치하는 DeadLetter를 조회한다")
    void findByIdAndClaimTokenAndDeadLetterStatus() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-16");
        String claimToken = "claim-token";

        updateProcessing(
                deadLetter.getId(),
                claimToken,
                LocalDateTime.now().plusMinutes(5)
        );

        entityManager.clear();

        // when
        var result = deadLetterRepository.findByIdAndClaimTokenAndDeadLetterStatus(
                deadLetter.getId(),
                claimToken,
                DeadLetterStatus.PROCESSING
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(deadLetter.getId());
    }

    @Test
    @DisplayName("존재하지 않는 ID로는 DeadLetter를 조회하지 않는다")
    void findByIdAndClaimTokenAndDeadLetterStatusWithInvalidId() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-17");

        String claimToken = "correct-token";

        updateProcessing(
                deadLetter.getId(),
                claimToken,
                getDatabaseTime().plusMinutes(5)
        );

        entityManager.clear();

        Long nonExistentId = 999L;

        // when
        var result = deadLetterRepository.findByIdAndClaimTokenAndDeadLetterStatus(
                nonExistentId,
                claimToken,
                DeadLetterStatus.PROCESSING
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상태가 일치하지 않는 DeadLetter는 조회하지 않는다")
    void findByIdAndClaimTokenAndDeadLetterStatusWithInvalidStatus() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-18");

        String claimToken = "correct-token";

        updateProcessing(
                deadLetter.getId(),
                claimToken,
                getDatabaseTime().plusMinutes(5)
        );

        entityManager.clear();

        // when
        var result = deadLetterRepository.findByIdAndClaimTokenAndDeadLetterStatus(
                deadLetter.getId(),
                claimToken,
                DeadLetterStatus.PENDING
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("잘못된 claimToken으로는 DeadLetter를 조회하지 않는다")
    void findByIdAndClaimTokenAndDeadLetterStatusWithInvalidClaimToken() {
        // given
        DeadLetterQueue deadLetter = saveDeadLetter("event-19");

        String claimToken = "correct-token";

        updateProcessing(
                deadLetter.getId(),
                claimToken,
                FUTURE_LEASE_UNTIL
        );

        entityManager.clear();

        // when
        var result = deadLetterRepository.findByIdAndClaimTokenAndDeadLetterStatus(
                deadLetter.getId(),
                "wrong-token",
                DeadLetterStatus.PROCESSING
        );

        // then
        assertThat(result).isEmpty();
    }

    // ==============================
    // Helper methods
    // ==============================

    private DeadLetterQueue saveDeadLetter(String eventId) {
        DeadLetterQueue deadLetter = DeadLetterQueue.create(
                DeadLetterType.FORCE_LOGOUT,
                DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER,
                DeadLetterAggregateType.SESSION,
                "aggregate-" + eventId,
                1L,
                "{\"sessionId\":\"session-1\"}",
                "처리 실패",
                "RuntimeException",
                0,
                eventId
        );

        return deadLetterRepository.saveAndFlush(deadLetter);
    }

    private void updateProcessing(
            Long deadLetterId,
            String claimToken,
            LocalDateTime leaseUntil
    ) {
        entityManager.createNativeQuery("""
                UPDATE dead_letter_queues
                SET dead_letter_status = 'PROCESSING',
                    claim_token = :claimToken,
                    lease_until = :leaseUntil
                WHERE id = :deadLetterId
                """)
                .setParameter("claimToken", claimToken)
                .setParameter("leaseUntil", leaseUntil)
                .setParameter("deadLetterId", deadLetterId)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    private DeadLetterQueue findById(Long deadLetterId) {
        return deadLetterRepository.findById(deadLetterId)
                .orElseThrow();
    }

    private LocalDateTime getDatabaseTime() {
        return ((java.sql.Timestamp) entityManager
                .createNativeQuery("SELECT LOCALTIMESTAMP")
                .getSingleResult())
                .toLocalDateTime();
    }
}