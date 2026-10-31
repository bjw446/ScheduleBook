package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ScheduleRepositoryTest {

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

    private Schedule createSchedule(
            User user,
            String title,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        Schedule schedule = Schedule.create(
                user,
                title,
                "일정 내용",
                scheduleDate,
                startTime,
                endTime
        );

        return scheduleRepository.save(schedule);
    }

    // --------------------------------------------------
    // findAllByUserIdAndScheduleDateBetween
    // --------------------------------------------------

    @Test
    @DisplayName("사용자 ID와 날짜 범위로 일정을 조회한다")
    void findAllByUserIdAndScheduleDateBetween() {
        // given
        User user = createUser("001");
        User otherUser = createUser("002");

        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        Schedule startSchedule = createSchedule(
                user, "시작일 일정",
                startDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        Schedule middleSchedule = createSchedule(
                user, "중간일 일정",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        Schedule endSchedule = createSchedule(
                user, "종료일 일정",
                endDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        createSchedule(
                user, "조회 범위 이전 일정",
                startDate.minusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        createSchedule(
                user, "조회 범위 이후 일정",
                endDate.plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        createSchedule(
                otherUser, "다른 사용자 일정",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // when
        List<Schedule> result =
                scheduleRepository.findAllByUserIdAndScheduleDateBetween(
                        user.getId(),
                        startDate,
                        endDate
                );

        // then
        assertThat(result)
                .hasSize(3)
                .extracting(Schedule::getId)
                .containsExactlyInAnyOrder(
                        startSchedule.getId(),
                        middleSchedule.getId(),
                        endSchedule.getId()
                );
    }

    // --------------------------------------------------
    // findByUser_IdAndScheduleDate
    // --------------------------------------------------

    @Test
    @DisplayName("사용자 ID와 특정 날짜로 일정을 조회한다")
    void findByUser_IdAndScheduleDate() {
        // given
        User user = createUser("003");
        User otherUser = createUser("004");

        LocalDate targetDate = LocalDate.of(2026, 10, 15);

        Schedule targetSchedule = createSchedule(
                user, "조회 대상 일정",
                targetDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        createSchedule(
                user, "다른 날짜 일정",
                targetDate.plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        createSchedule(
                otherUser, "다른 사용자 일정",
                targetDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // when
        List<Schedule> result =
                scheduleRepository.findByUser_IdAndScheduleDate(
                        user.getId(),
                        targetDate
                );

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(Schedule::getId)
                .containsExactly(targetSchedule.getId());
    }

    // --------------------------------------------------
    // increaseCommentCount
    // --------------------------------------------------

    @Test
    @DisplayName("일정의 댓글 수를 1 증가시킨다")
    void increaseCommentCount() {
        // given
        User user = createUser("005");

        Schedule schedule = createSchedule(
                user,
                "댓글 증가 테스트",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        entityManager.flush();

        // when
        scheduleRepository.increaseCommentCount(schedule.getId());

        entityManager.flush();
        entityManager.clear();

        // then
        int result = scheduleRepository.findCommentCount(schedule.getId());

        assertThat(result).isEqualTo(1);
    }

    // --------------------------------------------------
    // decreaseCommentCount
    // --------------------------------------------------

    @Test
    @DisplayName("일정의 댓글 수를 지정한 수만큼 감소시킨다")
    void decreaseCommentCount() {
        // given
        User user = createUser("006");

        Schedule schedule = createSchedule(
                user,
                "댓글 감소 테스트",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        for (int i = 0; i < 5; i++) {
            scheduleRepository.increaseCommentCount(schedule.getId());
        }

        entityManager.flush();

        // when
        scheduleRepository.decreaseCommentCount(schedule.getId(), 3);

        entityManager.flush();
        entityManager.clear();

        // then
        int result = scheduleRepository.findCommentCount(schedule.getId());

        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("현재 댓글 수보다 큰 값으로 감소시키면 댓글 수가 변경되지 않는다")
    void decreaseCommentCountWhenCountIsGreaterThanCommentCount() {
        // given
        User user = createUser("007");

        Schedule schedule = createSchedule(
                user,
                "댓글 감소 경계값 테스트",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        scheduleRepository.increaseCommentCount(schedule.getId());
        scheduleRepository.increaseCommentCount(schedule.getId());

        entityManager.flush();

        // when
        scheduleRepository.decreaseCommentCount(schedule.getId(), 3);

        entityManager.flush();
        entityManager.clear();

        // then
        int result = scheduleRepository.findCommentCount(schedule.getId());

        assertThat(result).isEqualTo(2);
    }

    // --------------------------------------------------
    // findWithOwner
    // --------------------------------------------------

    @Test
    @DisplayName("일정 조회 시 소유자 정보를 함께 조회한다")
    void findWithOwner() {
        // given
        User user = createUser("008");

        Schedule schedule = createSchedule(
                user,
                "소유자 조회 테스트",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Schedule> result =
                scheduleRepository.findWithOwner(schedule.getId());

        // then
        assertThat(result).isPresent();

        Schedule foundSchedule = result.get();

        assertThat(Hibernate.isInitialized(foundSchedule.getUser())).isTrue();
        assertThat(foundSchedule.getUser().getId()).isEqualTo(user.getId());
        assertThat(foundSchedule.getUser().getNickname()).isEqualTo("nickname008");
    }

    // --------------------------------------------------
    // findCommentCount
    // --------------------------------------------------

    @Test
    @DisplayName("일정의 댓글 수를 조회한다")
    void findCommentCount() {
        // given
        User user = createUser("009");

        Schedule schedule = createSchedule(
                user,
                "댓글 수 조회 테스트",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        for (int i = 0; i < 5; i++) {
            scheduleRepository.increaseCommentCount(schedule.getId());
        }

        entityManager.flush();
        entityManager.clear();

        // when
        int result = scheduleRepository.findCommentCount(schedule.getId());

        // then
        assertThat(result).isEqualTo(5);
    }

    // --------------------------------------------------
    // existsByIdAndUser_Id
    // --------------------------------------------------

    @Test
    @DisplayName("일정 ID와 소유자 ID가 일치하면 true를 반환한다")
    void existsByIdAndUser_IdWhenOwnerMatches() {
        // given
        User user = createUser("010");

        Schedule schedule = createSchedule(
                user,
                "소유자 확인 테스트",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // when
        boolean result = scheduleRepository.existsByIdAndUser_Id(
                schedule.getId(),
                user.getId()
        );

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("일정 소유자가 아니면 false를 반환한다")
    void existsByIdAndUser_IdWhenOwnerDoesNotMatch() {
        // given
        User user = createUser("011");
        User otherUser = createUser("012");

        Schedule schedule = createSchedule(
                user,
                "소유자가 아닌 사용자 확인 테스트",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // when
        boolean result = scheduleRepository.existsByIdAndUser_Id(
                schedule.getId(),
                otherUser.getId()
        );

        // then
        assertThat(result).isFalse();
    }

    // --------------------------------------------------
    // softDeleteAllByUserId
    // --------------------------------------------------

    @Test
    @DisplayName("사용자의 활성 일정을 일괄 소프트 삭제한다")
    void softDeleteAllByUserId() {
        // given
        User user = createUser("013");
        User otherUser = createUser("014");

        Schedule schedule1 = createSchedule(
                user,
                "첫 번째 일정",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        Schedule schedule2 = createSchedule(
                user,
                "두 번째 일정",
                LocalDate.of(2026, 10, 16),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        Schedule otherSchedule = createSchedule(
                otherUser,
                "다른 사용자 일정",
                LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        entityManager.flush();

        // when
        scheduleRepository.softDeleteAllByUserId(user.getId());

        entityManager.flush();
        entityManager.clear();

        // then
        Schedule deletedSchedule1 = entityManager.find(
                Schedule.class,
                schedule1.getId()
        );

        Schedule deletedSchedule2 = entityManager.find(
                Schedule.class,
                schedule2.getId()
        );

        Schedule activeSchedule = entityManager.find(
                Schedule.class,
                otherSchedule.getId()
        );

        assertThat(deletedSchedule1.isDeleted()).isTrue();
        assertThat(deletedSchedule1.getDeletedAt()).isNotNull();

        assertThat(deletedSchedule2.isDeleted()).isTrue();
        assertThat(deletedSchedule2.getDeletedAt()).isNotNull();

        assertThat(activeSchedule.isDeleted()).isFalse();
        assertThat(activeSchedule.getDeletedAt()).isNull();
    }
}