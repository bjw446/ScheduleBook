package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ScheduleReminderRepositoryTest {

    @Autowired
    private ScheduleReminderRepository scheduleReminderRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private EntityManager entityManager;

    private User createUser(String suffix) {
        User user = User.create(
                "login" + suffix,
                "encodedPassword",
                "nickname" + suffix,
                "email" + suffix + "@test.com",
                "0101234" + suffix
        );

        entityManager.persist(user);

        return user;
    }

    private Schedule createSchedule(User user, String title) {
        Schedule schedule = Schedule.create(
                user,
                title,
                "일정 내용",
                LocalDate.of(2026, 11, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        return scheduleRepository.save(schedule);
    }

    private ScheduleReminder createReminder(
            Schedule schedule,
            LocalDateTime reminderTime
    ) {
        ScheduleReminder reminder = ScheduleReminder.of(
                schedule,
                reminderTime
        );

        return scheduleReminderRepository.save(reminder);
    }

    private ScheduleReminder findReminder(Long reminderId) {
        entityManager.flush();
        entityManager.clear();

        return scheduleReminderRepository.findById(reminderId)
                .orElseThrow();
    }

    // --------------------------------------------------
    // deleteBySchedule_Id
    // --------------------------------------------------

    @Test
    @DisplayName("특정 일정에 등록된 알림을 모두 삭제한다")
    void deleteBySchedule_Id() {
        // given
        User user = createUser("001");

        Schedule schedule = createSchedule(user, "알림 삭제 대상 일정");
        Schedule otherSchedule = createSchedule(user, "다른 일정");

        LocalDateTime reminderTime =
                LocalDateTime.of(2026, 11, 15, 8, 0);

        ScheduleReminder reminder1 = createReminder(
                schedule,
                reminderTime
        );

        ScheduleReminder reminder2 = createReminder(
                schedule,
                reminderTime.plusMinutes(10)
        );

        ScheduleReminder otherReminder = createReminder(
                otherSchedule,
                reminderTime
        );

        entityManager.flush();

        // when
        scheduleReminderRepository.deleteBySchedule_Id(schedule.getId());

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(scheduleReminderRepository.findById(reminder1.getId()))
                .isEmpty();

        assertThat(scheduleReminderRepository.findById(reminder2.getId()))
                .isEmpty();

        assertThat(scheduleReminderRepository.findById(otherReminder.getId()))
                .isPresent();
    }

    // --------------------------------------------------
    // findReminders
    // --------------------------------------------------

    @Test
    @DisplayName("현재 시각 이전 또는 동일한 PENDING 알림을 조회한다")
    void findReminders() {
        // given
        User user = createUser("002");

        Schedule schedule = createSchedule(user, "알림 조회 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        ScheduleReminder pastReminder = createReminder(
                schedule,
                now.minusMinutes(10)
        );

        ScheduleReminder sameTimeReminder = createReminder(
                schedule,
                now
        );

        createReminder(
                schedule,
                now.plusMinutes(10)
        );

        ScheduleReminder processingReminder = createReminder(
                schedule,
                now.minusMinutes(20)
        );

        ScheduleReminder sentReminder = createReminder(
                schedule,
                now.minusMinutes(30)
        );

        entityManager.flush();

        scheduleReminderRepository.markProcessing(
                processingReminder.getId(),
                "processing-token",
                now.minusMinutes(5)
        );

        scheduleReminderRepository.markProcessing(
                sentReminder.getId(),
                "sent-token",
                now.minusMinutes(5)
        );

        scheduleReminderRepository.markSent(
                sentReminder.getId(),
                "sent-token"
        );

        entityManager.flush();
        entityManager.clear();

        // when
        List<ScheduleReminder> result =
                scheduleReminderRepository.findReminders(
                        now,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ScheduleReminder::getId)
                .containsExactly(
                        pastReminder.getId(),
                        sameTimeReminder.getId()
                );
    }

    @Test
    @DisplayName("알림 시간과 ID 오름차순으로 정렬하고 Pageable을 적용한다")
    void findRemindersWithSortingAndPaging() {
        // given
        User user = createUser("003");

        Schedule schedule = createSchedule(user, "알림 정렬 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        // ID가 가장 작은 알림
        ScheduleReminder firstReminder = createReminder(
                schedule,
                now.minusMinutes(15)
        );

        // ID가 두 번째로 작은 알림
        ScheduleReminder secondReminder = createReminder(
                schedule,
                now.minusMinutes(15)
        );

        // ID가 가장 큰 알림
        ScheduleReminder thirdReminder = createReminder(
                schedule,
                now.minusMinutes(15)
        );

        // 알림 시간은 모두 동일하지만 ID 순서는 생성 순서와 다르게 구성
        entityManager.flush();

        // when
        List<ScheduleReminder> result =
                scheduleReminderRepository.findReminders(
                        now,
                        PageRequest.of(0, 2)
                );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ScheduleReminder::getId)
                .containsExactly(
                        firstReminder.getId(),
                        secondReminder.getId()
                );

        assertThat(result.get(0).getId())
                .isLessThan(result.get(1).getId());

        assertThat(result)
                .extracting(ScheduleReminder::getId)
                .doesNotContain(thirdReminder.getId());
    }

    @Test
    @DisplayName("알림 조회 시 일정과 일정 소유자를 함께 조회한다")
    void findRemindersWithFetchJoin() {
        // given
        User user = createUser("004");

        Schedule schedule = createSchedule(user, "Fetch Join 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        createReminder(schedule, now.minusMinutes(10));

        entityManager.flush();
        entityManager.clear();

        // when
        List<ScheduleReminder> result =
                scheduleReminderRepository.findReminders(
                        now,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result).hasSize(1);

        ScheduleReminder reminder = result.get(0);

        assertThat(Hibernate.isInitialized(reminder.getSchedule()))
                .isTrue();

        assertThat(Hibernate.isInitialized(reminder.getSchedule().getUser()))
                .isTrue();

        assertThat(reminder.getSchedule().getUser().getId())
                .isEqualTo(user.getId());

        assertThat(reminder.getSchedule().getUser().getNickname())
                .isEqualTo("nickname004");
    }

    // --------------------------------------------------
    // markProcessing
    // --------------------------------------------------

    @Test
    @DisplayName("PENDING 알림을 PROCESSING 상태로 변경하고 Claim 정보를 저장한다")
    void markProcessing() {
        // given
        User user = createUser("005");

        Schedule schedule = createSchedule(user, "알림 처리 시작 테스트");

        LocalDateTime reminderTime =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        ScheduleReminder reminder = createReminder(
                schedule,
                reminderTime
        );

        String claimToken = "claim-token-001";

        LocalDateTime claimedAt = reminderTime.minusMinutes(5);

        entityManager.flush();

        // when
        int result = scheduleReminderRepository.markProcessing(
                reminder.getId(),
                claimToken,
                claimedAt
        );

        // then
        assertThat(result).isEqualTo(1);

        ScheduleReminder updatedReminder =
                findReminder(reminder.getId());

        assertThat(updatedReminder.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PROCESSING);

        assertThat(updatedReminder.getClaimToken())
                .isEqualTo(claimToken);

        assertThat(updatedReminder.getClaimedAt())
                .isEqualTo(claimedAt);
    }

    @Test
    @DisplayName("PENDING 상태가 아닌 알림은 PROCESSING 상태로 변경하지 않는다")
    void markProcessingWhenStatusIsNotPending() {
        // given
        User user = createUser("006");

        Schedule schedule = createSchedule(user, "중복 알림 처리 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        ScheduleReminder reminder = createReminder(
                schedule,
                now
        );

        String firstToken = "first-token";
        String secondToken = "second-token";

        entityManager.flush();

        scheduleReminderRepository.markProcessing(
                reminder.getId(),
                firstToken,
                now
        );

        entityManager.flush();

        // when
        int result = scheduleReminderRepository.markProcessing(
                reminder.getId(),
                secondToken,
                now.plusMinutes(1)
        );

        // then
        assertThat(result).isZero();

        ScheduleReminder updatedReminder =
                findReminder(reminder.getId());

        assertThat(updatedReminder.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PROCESSING);

        assertThat(updatedReminder.getClaimToken())
                .isEqualTo(firstToken);

        assertThat(updatedReminder.getClaimedAt())
                .isEqualTo(now);
    }

    // --------------------------------------------------
    // markSent
    // --------------------------------------------------

    @Test
    @DisplayName("올바른 Claim Token으로 PROCESSING 알림을 SENT 상태로 변경한다")
    void markSent() {
        // given
        User user = createUser("007");

        Schedule schedule = createSchedule(user, "알림 발송 완료 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        ScheduleReminder reminder = createReminder(schedule, now);

        String claimToken = "claim-token-002";

        entityManager.flush();

        scheduleReminderRepository.markProcessing(
                reminder.getId(),
                claimToken,
                now.minusMinutes(5)
        );

        entityManager.flush();

        // when
        int result = scheduleReminderRepository.markSent(
                reminder.getId(),
                claimToken
        );

        // then
        assertThat(result).isEqualTo(1);

        ScheduleReminder updatedReminder =
                findReminder(reminder.getId());

        assertThat(updatedReminder.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.SENT);

        assertThat(updatedReminder.getClaimToken()).isNull();

        assertThat(updatedReminder.getClaimedAt()).isNull();
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 SENT 상태로 변경하지 않는다")
    void markSentWhenClaimTokenDoesNotMatch() {
        // given
        User user = createUser("008");

        Schedule schedule = createSchedule(user, "Claim Token 불일치 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        ScheduleReminder reminder = createReminder(schedule, now);

        String correctToken = "correct-token";
        String wrongToken = "wrong-token";

        entityManager.flush();

        scheduleReminderRepository.markProcessing(
                reminder.getId(),
                correctToken,
                now.minusMinutes(5)
        );

        entityManager.flush();

        // when
        int result = scheduleReminderRepository.markSent(
                reminder.getId(),
                wrongToken
        );

        // then
        assertThat(result).isZero();

        ScheduleReminder updatedReminder =
                findReminder(reminder.getId());

        assertThat(updatedReminder.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PROCESSING);

        assertThat(updatedReminder.getClaimToken())
                .isEqualTo(correctToken);

        assertThat(updatedReminder.getClaimedAt())
                .isEqualTo(now.minusMinutes(5));
    }

    // --------------------------------------------------
    // markPending
    // --------------------------------------------------

    @Test
    @DisplayName("올바른 Claim Token으로 PROCESSING 알림을 PENDING 상태로 되돌린다")
    void markPending() {
        // given
        User user = createUser("009");

        Schedule schedule = createSchedule(user, "알림 재시도 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        ScheduleReminder reminder = createReminder(schedule, now);

        String claimToken = "claim-token-003";

        entityManager.flush();

        scheduleReminderRepository.markProcessing(
                reminder.getId(),
                claimToken,
                now.minusMinutes(5)
        );

        entityManager.flush();

        // when
        int result = scheduleReminderRepository.markPending(
                reminder.getId(),
                claimToken
        );

        // then
        assertThat(result).isEqualTo(1);

        ScheduleReminder updatedReminder =
                findReminder(reminder.getId());

        assertThat(updatedReminder.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PENDING);

        assertThat(updatedReminder.getClaimToken()).isNull();

        assertThat(updatedReminder.getClaimedAt()).isNull();
    }

    @Test
    @DisplayName("Claim Token이 일치하지 않으면 PENDING 상태로 변경하지 않는다")
    void markPendingWhenClaimTokenDoesNotMatch() {
        // given
        User user = createUser("010");

        Schedule schedule = createSchedule(user, "재시도 Claim Token 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        ScheduleReminder reminder = createReminder(schedule, now);

        String correctToken = "correct-token";
        String wrongToken = "wrong-token";

        entityManager.flush();

        scheduleReminderRepository.markProcessing(
                reminder.getId(),
                correctToken,
                now.minusMinutes(5)
        );

        entityManager.flush();

        // when
        int result = scheduleReminderRepository.markPending(
                reminder.getId(),
                wrongToken
        );

        // then
        assertThat(result).isZero();

        ScheduleReminder updatedReminder =
                findReminder(reminder.getId());

        assertThat(updatedReminder.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PROCESSING);

        assertThat(updatedReminder.getClaimToken())
                .isEqualTo(correctToken);
    }

    // --------------------------------------------------
    // recoverStuckReminders
    // --------------------------------------------------

    @Test
    @DisplayName("기준 시간보다 오래 처리 중인 알림을 PENDING 상태로 복구한다")
    void recoverStuckReminders() {
        // given
        User user = createUser("011");

        Schedule schedule = createSchedule(user, "알림 복구 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        LocalDateTime threshold = now.minusMinutes(10);

        ScheduleReminder stuckReminder = createReminder(
                schedule,
                now
        );

        ScheduleReminder boundaryReminder = createReminder(
                schedule,
                now.plusMinutes(1)
        );

        ScheduleReminder recentReminder = createReminder(
                schedule,
                now.plusMinutes(2)
        );

        ScheduleReminder pendingReminder = createReminder(
                schedule,
                now.plusMinutes(3)
        );

        entityManager.flush();

        scheduleReminderRepository.markProcessing(
                stuckReminder.getId(),
                "stuck-token",
                threshold.minusSeconds(1)
        );

        scheduleReminderRepository.markProcessing(
                boundaryReminder.getId(),
                "boundary-token",
                threshold
        );

        scheduleReminderRepository.markProcessing(
                recentReminder.getId(),
                "recent-token",
                threshold.plusSeconds(1)
        );

        entityManager.flush();

        // when
        int result =
                scheduleReminderRepository.recoverStuckReminders(threshold);

        // then
        assertThat(result).isEqualTo(1);

        ScheduleReminder recoveredReminder =
                findReminder(stuckReminder.getId());

        ScheduleReminder boundary =
                scheduleReminderRepository.findById(boundaryReminder.getId())
                        .orElseThrow();

        ScheduleReminder recent =
                scheduleReminderRepository.findById(recentReminder.getId())
                        .orElseThrow();

        ScheduleReminder pending =
                scheduleReminderRepository.findById(pendingReminder.getId())
                        .orElseThrow();

        assertThat(recoveredReminder.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PENDING);

        assertThat(recoveredReminder.getClaimToken()).isNull();

        assertThat(recoveredReminder.getClaimedAt()).isNull();

        assertThat(boundary.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PROCESSING);

        assertThat(recent.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PROCESSING);

        assertThat(pending.getScheduleReminderStatus())
                .isEqualTo(ScheduleReminderStatus.PENDING);
    }

    // --------------------------------------------------
    // countByScheduleReminderStatus
    // --------------------------------------------------

    @Test
    @DisplayName("알림 상태별 개수를 조회한다")
    void countByScheduleReminderStatus() {
        // given
        User user = createUser("012");

        Schedule schedule = createSchedule(user, "알림 상태별 개수 테스트");

        LocalDateTime now =
                LocalDateTime.of(2026, 11, 15, 9, 0);

        ScheduleReminder pendingReminder1 = createReminder(
                schedule,
                now
        );

        ScheduleReminder pendingReminder2 = createReminder(
                schedule,
                now.plusMinutes(1)
        );

        ScheduleReminder processingReminder = createReminder(
                schedule,
                now.plusMinutes(2)
        );

        ScheduleReminder sentReminder = createReminder(
                schedule,
                now.plusMinutes(3)
        );

        entityManager.flush();

        scheduleReminderRepository.markProcessing(
                processingReminder.getId(),
                "processing-token",
                now
        );

        scheduleReminderRepository.markProcessing(
                sentReminder.getId(),
                "sent-token",
                now
        );

        scheduleReminderRepository.markSent(
                sentReminder.getId(),
                "sent-token"
        );

        entityManager.flush();
        entityManager.clear();

        // when
        long pendingCount =
                scheduleReminderRepository.countByScheduleReminderStatus(
                        ScheduleReminderStatus.PENDING
                );

        long processingCount =
                scheduleReminderRepository.countByScheduleReminderStatus(
                        ScheduleReminderStatus.PROCESSING
                );

        long sentCount =
                scheduleReminderRepository.countByScheduleReminderStatus(
                        ScheduleReminderStatus.SENT
                );

        // then
        assertThat(pendingCount).isEqualTo(2);
        assertThat(processingCount).isEqualTo(1);
        assertThat(sentCount).isEqualTo(1);
    }
}