package com.example.schedulebook.domain.scheduleparticipant.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleparticipant.enums.AttendanceStatus;
import com.example.schedulebook.domain.scheduleparticipant.projection.ScheduleParticipantProjection;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ScheduleParticipantRepositoryTest {

    @Autowired
    private ScheduleParticipantRepository scheduleParticipantRepository;

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

    private ScheduleParticipant createParticipant(
            Schedule schedule,
            User user
    ) {
        ScheduleParticipant participant = ScheduleParticipant.of(
                schedule,
                user
        );

        return scheduleParticipantRepository.save(participant);
    }

    // --------------------------------------------------
    // findBySchedule_IdAndUser_Id
    // --------------------------------------------------

    @Test
    @DisplayName("일정 ID와 사용자 ID로 일정 참여자를 조회한다")
    void findBySchedule_IdAndUser_Id() {
        // given
        User owner = createUser("001");
        User participant = createUser("002");

        Schedule schedule = createSchedule(owner, "참여자 조회 테스트");

        ScheduleParticipant savedParticipant =
                createParticipant(schedule, participant);

        entityManager.flush();
        entityManager.clear();

        // when
        var result = scheduleParticipantRepository
                .findBySchedule_IdAndUser_Id(
                        schedule.getId(),
                        participant.getId()
                );

        // then
        assertThat(result).isPresent();

        assertThat(result.get().getId())
                .isEqualTo(savedParticipant.getId());

        assertThat(result.get().getSchedule().getId())
                .isEqualTo(schedule.getId());

        assertThat(result.get().getUser().getId())
                .isEqualTo(participant.getId());
    }

    @Test
    @DisplayName("일정에 참여하지 않은 사용자를 조회하면 빈 Optional을 반환한다")
    void findBySchedule_IdAndUser_IdWhenNotExists() {
        // given
        User owner = createUser("003");
        User participant = createUser("004");

        Schedule schedule = createSchedule(owner, "참여자 미존재 테스트");

        entityManager.flush();

        // when
        var result = scheduleParticipantRepository
                .findBySchedule_IdAndUser_Id(
                        schedule.getId(),
                        participant.getId()
                );

        // then
        assertThat(result).isEmpty();
    }

    // --------------------------------------------------
    // findParticipantIds
    // --------------------------------------------------

    @Test
    @DisplayName("특정 일정에 참여한 사용자 ID 목록을 조회한다")
    void findParticipantIds() {
        // given
        User owner = createUser("005");
        User participant1 = createUser("006");
        User participant2 = createUser("007");
        User otherUser = createUser("008");

        Schedule schedule = createSchedule(owner, "참여자 ID 조회 테스트");
        Schedule otherSchedule = createSchedule(owner, "다른 일정");

        createParticipant(schedule, participant1);
        createParticipant(schedule, participant2);

        createParticipant(otherSchedule, otherUser);

        entityManager.flush();

        // when
        List<Long> result =
                scheduleParticipantRepository.findParticipantIds(
                        schedule.getId()
                );

        // then
        assertThat(result)
                .containsExactlyInAnyOrder(
                        participant1.getId(),
                        participant2.getId()
                );

        assertThat(result)
                .doesNotContain(otherUser.getId());
    }

    @Test
    @DisplayName("참여자가 없는 일정의 사용자 ID 목록은 빈 리스트를 반환한다")
    void findParticipantIdsWhenEmpty() {
        // given
        User owner = createUser("009");

        Schedule schedule = createSchedule(owner, "참여자 없는 일정");

        entityManager.flush();

        // when
        List<Long> result =
                scheduleParticipantRepository.findParticipantIds(
                        schedule.getId()
                );

        // then
        assertThat(result).isEmpty();
    }

    // --------------------------------------------------
    // existsBySchedule_IdAndUser_Id
    // --------------------------------------------------

    @Test
    @DisplayName("특정 사용자의 일정 참여 여부를 확인한다")
    void existsBySchedule_IdAndUser_Id() {
        // given
        User owner = createUser("010");
        User participant = createUser("011");
        User otherUser = createUser("012");

        Schedule schedule = createSchedule(owner, "참여 여부 확인 테스트");

        createParticipant(schedule, participant);

        entityManager.flush();

        // when
        boolean exists = scheduleParticipantRepository
                .existsBySchedule_IdAndUser_Id(
                        schedule.getId(),
                        participant.getId()
                );

        boolean notExists = scheduleParticipantRepository
                .existsBySchedule_IdAndUser_Id(
                        schedule.getId(),
                        otherUser.getId()
                );

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // --------------------------------------------------
    // findParticipants
    // --------------------------------------------------

    @Test
    @DisplayName("일정 참여자의 사용자 정보와 소유자 여부 및 출석 상태를 조회한다")
    void findParticipants() {
        // given
        User owner = createUser("013");
        User participant1 = createUser("014");
        User participant2 = createUser("015");

        Schedule schedule = createSchedule(owner, "참여자 Projection 테스트");

        ScheduleParticipant ownerParticipant =
                createParticipant(schedule, owner);

        ScheduleParticipant firstParticipant =
                createParticipant(schedule, participant1);

        ScheduleParticipant secondParticipant =
                createParticipant(schedule, participant2);

        firstParticipant.updateAttendanceStatus(AttendanceStatus.ACCEPTED);
        secondParticipant.updateAttendanceStatus(AttendanceStatus.DECLINED);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ScheduleParticipantProjection> result =
                scheduleParticipantRepository.findParticipants(
                        schedule.getId()
                );

        // then
        assertThat(result).hasSize(3);

        // 참여자 ID 순서대로 조회되는지 검증
        assertThat(result)
                .extracting(ScheduleParticipantProjection::getUserId)
                .containsExactly(
                        owner.getId(),
                        participant1.getId(),
                        participant2.getId()
                );

        // 소유자 정보 검증
        assertThat(result.get(0).getNickname())
                .isEqualTo(owner.getNickname());

        assertThat(result.get(0).isOwner()).isTrue();

        assertThat(result.get(0).getAttendanceStatus())
                .isEqualTo(AttendanceStatus.PENDING);

        // 첫 번째 일반 참여자 검증
        assertThat(result.get(1).getNickname())
                .isEqualTo(participant1.getNickname());

        assertThat(result.get(1).isOwner()).isFalse();

        assertThat(result.get(1).getAttendanceStatus())
                .isEqualTo(AttendanceStatus.ACCEPTED);

        // 두 번째 일반 참여자 검증
        assertThat(result.get(2).getNickname())
                .isEqualTo(participant2.getNickname());

        assertThat(result.get(2).isOwner()).isFalse();

        assertThat(result.get(2).getAttendanceStatus())
                .isEqualTo(AttendanceStatus.DECLINED);
    }

    @Test
    @DisplayName("참여자가 없는 일정은 빈 Projection 목록을 반환한다")
    void findParticipantsWhenEmpty() {
        // given
        User owner = createUser("016");

        Schedule schedule = createSchedule(owner, "빈 참여자 Projection 테스트");

        entityManager.flush();

        // when
        List<ScheduleParticipantProjection> result =
                scheduleParticipantRepository.findParticipants(
                        schedule.getId()
                );

        // then
        assertThat(result).isEmpty();
    }

    // --------------------------------------------------
    // findAllBySchedule_Id
    // --------------------------------------------------

    @Test
    @DisplayName("특정 일정에 등록된 모든 참여자를 조회한다")
    void findAllBySchedule_Id() {
        // given
        User owner = createUser("017");
        User participant1 = createUser("018");
        User participant2 = createUser("019");
        User otherParticipant = createUser("020");

        Schedule schedule = createSchedule(owner, "전체 참여자 조회 테스트");
        Schedule otherSchedule = createSchedule(owner, "다른 일정");

        ScheduleParticipant firstParticipant =
                createParticipant(schedule, participant1);

        ScheduleParticipant secondParticipant =
                createParticipant(schedule, participant2);

        createParticipant(otherSchedule, otherParticipant);

        entityManager.flush();

        // when
        List<ScheduleParticipant> result =
                scheduleParticipantRepository.findAllBySchedule_Id(
                        schedule.getId()
                );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ScheduleParticipant::getId)
                .containsExactlyInAnyOrder(
                        firstParticipant.getId(),
                        secondParticipant.getId()
                );
    }

    // --------------------------------------------------
    // deleteAllByUserId
    // --------------------------------------------------

    @Test
    @DisplayName("특정 사용자가 참여한 모든 일정의 참여 정보를 삭제한다")
    void deleteAllByUserId() {
        // given
        User owner = createUser("021");
        User participant = createUser("022");
        User otherParticipant = createUser("023");

        Schedule schedule1 = createSchedule(owner, "첫 번째 일정");
        Schedule schedule2 = createSchedule(owner, "두 번째 일정");

        ScheduleParticipant target1 =
                createParticipant(schedule1, participant);

        ScheduleParticipant target2 =
                createParticipant(schedule2, participant);

        ScheduleParticipant other =
                createParticipant(schedule1, otherParticipant);

        entityManager.flush();

        // when
        scheduleParticipantRepository.deleteAllByUserId(
                participant.getId()
        );

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(scheduleParticipantRepository.findById(target1.getId()))
                .isEmpty();

        assertThat(scheduleParticipantRepository.findById(target2.getId()))
                .isEmpty();

        assertThat(scheduleParticipantRepository.findById(other.getId()))
                .isPresent();
    }

    // --------------------------------------------------
    // deleteByOwnedSchedule
    // --------------------------------------------------

    @Test
    @DisplayName("특정 사용자가 소유한 일정의 모든 참여 정보를 삭제한다")
    void deleteByOwnedSchedule() {
        // given
        User owner = createUser("024");
        User otherOwner = createUser("025");
        User participant = createUser("026");

        Schedule ownedSchedule1 =
                createSchedule(owner, "첫 번째 소유 일정");

        Schedule ownedSchedule2 =
                createSchedule(owner, "두 번째 소유 일정");

        Schedule otherSchedule =
                createSchedule(otherOwner, "다른 사용자의 소유 일정");

        ScheduleParticipant ownedParticipant1 =
                createParticipant(ownedSchedule1, participant);

        ScheduleParticipant ownedParticipant2 =
                createParticipant(ownedSchedule2, participant);

        ScheduleParticipant otherParticipant =
                createParticipant(otherSchedule, participant);

        entityManager.flush();

        // when
        scheduleParticipantRepository.deleteByOwnedSchedule(
                owner.getId()
        );

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(scheduleParticipantRepository
                .findById(ownedParticipant1.getId()))
                .isEmpty();

        assertThat(scheduleParticipantRepository
                .findById(ownedParticipant2.getId()))
                .isEmpty();

        assertThat(scheduleParticipantRepository
                .findById(otherParticipant.getId()))
                .isPresent();
    }
}