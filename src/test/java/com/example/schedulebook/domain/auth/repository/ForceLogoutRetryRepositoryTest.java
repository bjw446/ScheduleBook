package com.example.schedulebook.domain.auth.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import com.example.schedulebook.domain.auth.enums.ForceLogoutRetryStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@Transactional
@DisplayName("ForceLogoutRetryRepository 테스트")
class ForceLogoutRetryRepositoryTest {

    @Autowired
    private ForceLogoutRetryRepository forceLogoutRetryRepository;

    @Autowired
    private EntityManager entityManager;

    private ForceLogoutRetry createRetry(
            String eventId,
            String sessionId,
            Long userId,
            String reason
    ) {
        return ForceLogoutRetry.create(
                eventId,
                sessionId,
                userId,
                "{\"sessionId\":\"" + sessionId + "\"}",
                reason
        );
    }

    private ForceLogoutRetry saveRetry(
            String eventId,
            String sessionId,
            Long userId,
            String reason
    ) {
        return forceLogoutRetryRepository.save(
                createRetry(eventId, sessionId, userId, reason)
        );
    }

    private void updateRetry(
            Long id,
            ForceLogoutRetryStatus status,
            int retryCount,
            LocalDateTime nextRetryAt,
            String reason,
            String claimToken,
            LocalDateTime processingAt
    ) {
        entityManager.createNativeQuery("""
                UPDATE force_logout_retry
                SET force_logout_retry_status = :status,
                    retry_count = :retryCount,
                    next_retry_at = :nextRetryAt,
                    reason = :reason,
                    claim_token = :claimToken,
                    processing_at = :processingAt
                WHERE id = :id
                """)
                .setParameter("status", status.name())
                .setParameter("retryCount", retryCount)
                .setParameter("nextRetryAt", nextRetryAt)
                .setParameter("reason", reason)
                .setParameter("claimToken", claimToken)
                .setParameter("processingAt", processingAt)
                .setParameter("id", id)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    private ForceLogoutRetry findById(Long id) {
        return forceLogoutRetryRepository.findById(id).orElseThrow();
    }

    @Test
    @DisplayName("올바른 Claim Token으로 SUCCESS 상태 변경 및 관련 필드 초기화")
    void markSuccess() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, "기존 사유");

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                2,
                LocalDateTime.now().minusMinutes(1),
                "기존 사유",
                "claim-token-1",
                LocalDateTime.now().minusMinutes(1)
        );

        // when
        int result = forceLogoutRetryRepository.markSuccess(
                retry.getId(),
                "claim-token-1"
        );

        // then
        assertThat(result).isEqualTo(1);

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.SUCCESS);
        assertThat(found.getReason()).isNull();
        assertThat(found.getNextRetryAt()).isNull();
        assertThat(found.getClaimToken()).isNull();
        assertThat(found.getProcessingAt()).isNull();
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 SUCCESS 상태 변경을 수행하지 않는다")
    void markSuccessWhenClaimTokenIsDifferent() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, "기존 사유");

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                0,
                LocalDateTime.now(),
                "기존 사유",
                "claim-token-1",
                LocalDateTime.now()
        );

        // when
        int result = forceLogoutRetryRepository.markSuccess(
                retry.getId(),
                "wrong-token"
        );

        // then
        assertThat(result).isZero();

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo("claim-token-1");
        assertThat(found.getReason()).isEqualTo("기존 사유");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 SUCCESS 상태 변경을 요청하면 변경하지 않는다")
    void markSuccessWhenIdDoesNotExist() {
        // given
        Long notFoundId = 999L;

        // when
        int result = forceLogoutRetryRepository.markSuccess(notFoundId, "claim-token");

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("올바른 Claim Token으로 FAILED 상태 변경 및 실패 사유 저장")
    void markFailed() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                1,
                LocalDateTime.now(),
                null,
                "claim-token-1",
                LocalDateTime.now()
        );

        // when
        int result = forceLogoutRetryRepository.markFailed(
                retry.getId(),
                "로그아웃 처리 실패",
                "claim-token-1"
        );

        // then
        assertThat(result).isEqualTo(1);

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.FAILED);
        assertThat(found.getReason()).isEqualTo("로그아웃 처리 실패");
        assertThat(found.getNextRetryAt()).isNull();
        assertThat(found.getProcessingAt()).isNull();
        assertThat(found.getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 FAILED 상태 변경을 수행하지 않는다")
    void markFailedWhenClaimTokenIsDifferent() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                0,
                null,
                null,
                "claim-token-1",
                LocalDateTime.now()
        );

        // when
        int result = forceLogoutRetryRepository.markFailed(
                retry.getId(),
                "로그아웃 처리 실패",
                "wrong-token"
        );

        // then
        assertThat(result).isZero();

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo("claim-token-1");
        assertThat(found.getReason()).isNull();
    }

    @Test
    @DisplayName("올바른 Claim Token으로 PENDING 상태 변경 및 재시도 횟수 증가")
    void markRetry() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                2,
                null,
                null,
                "claim-token-1",
                LocalDateTime.now()
        );

        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(5);

        // when
        int result = forceLogoutRetryRepository.markRetry(
                retry.getId(),
                "재시도 예정",
                nextRetryAt,
                "claim-token-1"
        );

        // then
        assertThat(result).isEqualTo(1);

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PENDING);
        assertThat(found.getRetryCount()).isEqualTo(3);
        assertThat(found.getReason()).isEqualTo("재시도 예정");
        assertThat(found.getNextRetryAt())
                .isCloseTo(nextRetryAt, within(1, ChronoUnit.MICROS));
        assertThat(found.getProcessingAt()).isNull();
        assertThat(found.getClaimToken()).isNull();
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 재시도 상태 변경을 수행하지 않는다")
    void markRetryWhenClaimTokenIsDifferent() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                2,
                null,
                null,
                "claim-token-1",
                LocalDateTime.now()
        );

        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(5);

        // when
        int result = forceLogoutRetryRepository.markRetry(
                retry.getId(),
                "재시도 예정",
                nextRetryAt,
                "wrong-token"
        );

        // then
        assertThat(result).isZero();

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PROCESSING);
        assertThat(found.getRetryCount()).isEqualTo(2);
        assertThat(found.getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("재시도 가능한 PENDING 상태를 PROCESSING으로 변경")
    void markProcessingWhenPendingIsRetryable() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, "기존 사유");

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PENDING,
                1,
                LocalDateTime.now().minusMinutes(1),
                "기존 사유",
                null,
                null
        );

        // when
        int result = forceLogoutRetryRepository.markProcessing(
                retry.getId(),
                "claim-token-1",
                LocalDateTime.now().minusMinutes(5)
        );

        // then
        assertThat(result).isEqualTo(1);

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo("claim-token-1");
        assertThat(found.getReason()).isNull();
        assertThat(found.getNextRetryAt()).isNull();
        assertThat(found.getProcessingAt()).isNotNull();
    }

    @Test
    @DisplayName("재시도 시간이 지나지 않은 PENDING 상태는 PROCESSING으로 변경하지 않는다")
    void markProcessingWhenNextRetryAtIsFuture() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, "기존 사유");

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PENDING,
                1,
                LocalDateTime.now().plusMinutes(10),
                "기존 사유",
                null,
                null
        );

        // when
        int result = forceLogoutRetryRepository.markProcessing(
                retry.getId(),
                "claim-token-1",
                LocalDateTime.now().minusMinutes(5)
        );

        // then
        assertThat(result).isZero();

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PENDING);
        assertThat(found.getClaimToken()).isNull();
    }

    @Test
    @DisplayName("재시도 시간이 NULL인 PENDING 상태는 PROCESSING으로 변경")
    void markProcessingWhenNextRetryAtIsNull() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, "기존 사유");

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PENDING,
                0,
                null,
                "기존 사유",
                null,
                null
        );

        // when
        int result = forceLogoutRetryRepository.markProcessing(
                retry.getId(),
                "claim-token-1",
                LocalDateTime.now().minusMinutes(5)
        );

        // then
        assertThat(result).isEqualTo(1);

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("Timeout이 지난 PROCESSING 상태를 새로운 Claim Token으로 재처리")
    void markProcessingWhenTimeoutExpired() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                1,
                null,
                null,
                "old-token",
                LocalDateTime.now().minusMinutes(10)
        );

        // when
        int result = forceLogoutRetryRepository.markProcessing(
                retry.getId(),
                "new-token",
                LocalDateTime.now().minusMinutes(5)
        );

        // then
        assertThat(result).isEqualTo(1);

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo("new-token");
        assertThat(found.getProcessingAt()).isNotNull();
    }

    @Test
    @DisplayName("Timeout이 지나지 않은 PROCESSING 상태는 재처리하지 않는다")
    void markProcessingWhenTimeoutNotExpired() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                1,
                null,
                null,
                "old-token",
                LocalDateTime.now().minusMinutes(1)
        );

        // when
        int result = forceLogoutRetryRepository.markProcessing(
                retry.getId(),
                "new-token",
                LocalDateTime.now().minusMinutes(5)
        );

        // then
        assertThat(result).isZero();

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PROCESSING);
        assertThat(found.getClaimToken()).isEqualTo("old-token");
    }

    @Test
    @DisplayName("PENDING 및 Timeout이 지난 PROCESSING 상태의 재시도 대상 조회")
    void findRetryTargets() {
        // given
        ForceLogoutRetry pending = saveRetry("event-1", "session-1", 1L, null);
        ForceLogoutRetry processing = saveRetry("event-2", "session-2", 2L, null);
        ForceLogoutRetry futurePending = saveRetry("event-3", "session-3", 3L, null);
        ForceLogoutRetry activeProcessing = saveRetry("event-4", "session-4", 4L, null);
        ForceLogoutRetry success = saveRetry("event-5", "session-5", 5L, null);
        ForceLogoutRetry failed = saveRetry("event-6", "session-6", 6L, null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeout = now.minusMinutes(5);

        updateRetry(
                pending.getId(),
                ForceLogoutRetryStatus.PENDING,
                0,
                now.minusMinutes(2),
                null,
                null,
                null
        );

        updateRetry(
                processing.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                1,
                now.minusMinutes(1),
                null,
                "token-2",
                now.minusMinutes(10)
        );

        updateRetry(
                futurePending.getId(),
                ForceLogoutRetryStatus.PENDING,
                0,
                now.plusMinutes(10),
                null,
                null,
                null
        );

        updateRetry(
                activeProcessing.getId(),
                ForceLogoutRetryStatus.PROCESSING,
                0,
                now.minusMinutes(3),
                null,
                "token-4",
                now.minusMinutes(1)
        );

        updateRetry(
                success.getId(),
                ForceLogoutRetryStatus.SUCCESS,
                0,
                now.minusMinutes(3),
                null,
                null,
                null
        );

        updateRetry(
                failed.getId(),
                ForceLogoutRetryStatus.FAILED,
                0,
                now.minusMinutes(4),
                null,
                null,
                null
        );

        // when
        Page<ForceLogoutRetry> result = forceLogoutRetryRepository.findRetryTargets(
                timeout,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent())
                .extracting(ForceLogoutRetry::getId)
                .containsExactly(pending.getId(), processing.getId());

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("재시도 대상 조회 시 nextRetryAt, retryCount, ID 기준 정렬")
    void findRetryTargetsWithSorting() {
        // given
        ForceLogoutRetry fourth = saveRetry("event-4", "session-4", 4L, null);
        ForceLogoutRetry third = saveRetry("event-3", "session-3", 3L, null);
        ForceLogoutRetry second = saveRetry("event-2", "session-2", 2L, null);
        ForceLogoutRetry first = saveRetry("event-1", "session-1", 1L, null);

        LocalDateTime firstTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime secondTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime thirdTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime fourthTime = LocalDateTime.of(2026, 1, 1, 11, 0);

        updateRetry(first.getId(), ForceLogoutRetryStatus.PENDING, 1,
                firstTime, null, null, null);

        updateRetry(second.getId(), ForceLogoutRetryStatus.PENDING, 1,
                secondTime, null, null, null);

        updateRetry(third.getId(), ForceLogoutRetryStatus.PENDING, 2,
                thirdTime, null, null, null);

        updateRetry(fourth.getId(), ForceLogoutRetryStatus.PENDING, 0,
                fourthTime, null, null, null);

        // when
        Page<ForceLogoutRetry> result = forceLogoutRetryRepository.findRetryTargets(
                LocalDateTime.now().minusMinutes(5),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent())
                .extracting(ForceLogoutRetry::getId)
                .containsExactly(
                        second.getId(),
                        first.getId(),
                        third.getId(),
                        fourth.getId()
                );
    }

    @Test
    @DisplayName("재시도 대상 페이징 조회")
    void findRetryTargetsWithPaging() {
        // given
        ForceLogoutRetry retry1 = saveRetry("event-1", "session-1", 1L, null);
        ForceLogoutRetry retry2 = saveRetry("event-2", "session-2", 2L, null);
        ForceLogoutRetry retry3 = saveRetry("event-3", "session-3", 3L, null);

        LocalDateTime retryTime = LocalDateTime.now().minusMinutes(1);

        updateRetry(
                retry1.getId(),
                ForceLogoutRetryStatus.PENDING,
                0,
                retryTime,
                null,
                null,
                null
        );

        updateRetry(
                retry2.getId(),
                ForceLogoutRetryStatus.PENDING,
                0,
                retryTime,
                null,
                null,
                null
        );

        updateRetry(
                retry3.getId(),
                ForceLogoutRetryStatus.PENDING,
                0,
                retryTime,
                null,
                null,
                null
        );

        // when
        Page<ForceLogoutRetry> firstPage = forceLogoutRetryRepository.findRetryTargets(
                LocalDateTime.now().minusMinutes(5),
                PageRequest.of(0, 2)
        );

        Page<ForceLogoutRetry> secondPage = forceLogoutRetryRepository.findRetryTargets(
                LocalDateTime.now().minusMinutes(5),
                PageRequest.of(1, 2)
        );

        // then
        assertThat(firstPage.getContent())
                .extracting(ForceLogoutRetry::getId)
                .containsExactly(retry1.getId(), retry2.getId());

        assertThat(secondPage.getContent())
                .extracting(ForceLogoutRetry::getId)
                .containsExactly(retry3.getId());

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);

        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("FAILED 상태의 재시도 데이터를 PENDING 상태로 복구")
    void updateRecover() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, "실패 사유");

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.FAILED,
                5,
                LocalDateTime.now().minusMinutes(1),
                "실패 사유",
                "claim-token-1",
                null
        );

        // when
        int result = forceLogoutRetryRepository.updateRecover(
                retry.getId(),
                "claim-token-1"
        );

        // then
        assertThat(result).isEqualTo(1);

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PENDING);
        assertThat(found.getRetryCount()).isZero();
        assertThat(found.getReason()).isNull();
        assertThat(found.getNextRetryAt()).isNotNull();
        assertThat(found.getClaimToken()).isNull();
        assertThat(found.getProcessingAt()).isNull();
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 재시도 데이터 복구를 수행하지 않는다")
    void updateRecoverWhenClaimTokenIsDifferent() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, "실패 사유");

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.FAILED,
                5,
                LocalDateTime.now(),
                "실패 사유",
                "claim-token-1",
                null
        );

        // when
        int result = forceLogoutRetryRepository.updateRecover(
                retry.getId(),
                "wrong-token"
        );

        // then
        assertThat(result).isZero();

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.FAILED);
        assertThat(found.getRetryCount()).isEqualTo(5);
        assertThat(found.getClaimToken()).isEqualTo("claim-token-1");
    }

    @Test
    @DisplayName("FAILED 상태가 아닌 데이터는 재시도 복구하지 않는다")
    void updateRecoverWhenStatusIsNotFailed() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        updateRetry(
                retry.getId(),
                ForceLogoutRetryStatus.PENDING,
                2,
                LocalDateTime.now(),
                null,
                "claim-token-1",
                null
        );

        // when
        int result = forceLogoutRetryRepository.updateRecover(
                retry.getId(),
                "claim-token-1"
        );

        // then
        assertThat(result).isZero();

        ForceLogoutRetry found = findById(retry.getId());

        assertThat(found.getForceLogoutRetryStatus()).isEqualTo(ForceLogoutRetryStatus.PENDING);
        assertThat(found.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Session ID를 통한 재시도 데이터 조회")
    void findBySessionId() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        // when
        Optional<ForceLogoutRetry> result = forceLogoutRetryRepository.findBySessionId("session-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(retry.getId());
        assertThat(result.get().getSessionId()).isEqualTo("session-1");
    }

    @Test
    @DisplayName("존재하지 않는 Session ID 조회 시 빈 Optional 반환")
    void findBySessionIdWhenNotFound() {
        // given
        saveRetry("event-1", "session-1", 1L, null);

        // when
        Optional<ForceLogoutRetry> result = forceLogoutRetryRepository.findBySessionId("unknown-session");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Event ID를 통한 재시도 데이터 조회")
    void findByEventId() {
        // given
        ForceLogoutRetry retry = saveRetry("event-1", "session-1", 1L, null);

        // when
        Optional<ForceLogoutRetry> result = forceLogoutRetryRepository.findByEventId("event-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(retry.getId());
        assertThat(result.get().getEventId()).isEqualTo("event-1");
    }

    @Test
    @DisplayName("존재하지 않는 Event ID 조회 시 빈 Optional 반환")
    void findByEventIdWhenNotFound() {
        // given
        saveRetry("event-1", "session-1", 1L, null);

        // when
        Optional<ForceLogoutRetry> result = forceLogoutRetryRepository.findByEventId("unknown-event");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("PENDING 상태의 재시도 데이터 개수 조회")
    void countPending() {
        // given
        ForceLogoutRetry pending1 = saveRetry("event-1", "session-1", 1L, null);
        ForceLogoutRetry pending2 = saveRetry("event-2", "session-2", 2L, null);
        ForceLogoutRetry processing = saveRetry("event-3", "session-3", 3L, null);
        ForceLogoutRetry success = saveRetry("event-4", "session-4", 4L, null);
        ForceLogoutRetry failed = saveRetry("event-5", "session-5", 5L, null);

        updateRetry(pending1.getId(), ForceLogoutRetryStatus.PENDING, 0,
                null, null, null, null);
        updateRetry(pending2.getId(), ForceLogoutRetryStatus.PENDING, 0,
                null, null, null, null);
        updateRetry(processing.getId(), ForceLogoutRetryStatus.PROCESSING, 0,
                null, null, "token-3", LocalDateTime.now());
        updateRetry(success.getId(), ForceLogoutRetryStatus.SUCCESS, 0,
                null, null, null, null);
        updateRetry(failed.getId(), ForceLogoutRetryStatus.FAILED, 0,
                null, null, null, null);

        // when
        long result = forceLogoutRetryRepository.countPending();

        // then
        assertThat(result).isEqualTo(2);
    }
}