package com.example.schedulebook.domain.scheduleshare.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ScheduleShareRepositoryTest {

    @Autowired
    private ScheduleShareRepository scheduleShareRepository;

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

    private ScheduleShare createShare(
            Schedule schedule,
            User sharedUser
    ) {
        ScheduleShare scheduleShare = ScheduleShare.create(
                schedule,
                sharedUser
        );

        return scheduleShareRepository.save(scheduleShare);
    }

    // --------------------------------------------------
    // findRelation
    // --------------------------------------------------

    @Test
    @DisplayName("일정 ID와 공유받은 사용자 ID로 공유 관계를 조회한다")
    void findRelation() {
        // given
        User owner = createUser("001");
        User sharedUser = createUser("002");

        Schedule schedule = createSchedule(owner, "공유 일정");

        ScheduleShare savedShare = createShare(schedule, sharedUser);

        entityManager.flush();
        entityManager.clear();

        // when
        var result = scheduleShareRepository.findRelation(
                schedule.getId(),
                sharedUser.getId()
        );

        // then
        assertThat(result).isPresent();

        assertThat(result.get().getId())
                .isEqualTo(savedShare.getId());

        assertThat(result.get().getSchedule().getId())
                .isEqualTo(schedule.getId());

        assertThat(result.get().getSharedUser().getId())
                .isEqualTo(sharedUser.getId());
    }

    @Test
    @DisplayName("다른 일정의 공유 관계는 조회되지 않는다")
    void findRelationWhenScheduleNotMatch() {
        // given
        User owner = createUser("003");
        User sharedUser = createUser("004");

        Schedule schedule = createSchedule(owner, "조회 대상 일정");
        Schedule otherSchedule = createSchedule(owner, "다른 일정");

        createShare(otherSchedule, sharedUser);

        entityManager.flush();

        // when
        var result = scheduleShareRepository.findRelation(
                schedule.getId(),
                sharedUser.getId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자에게 공유된 관계는 조회되지 않는다")
    void findRelationWhenUserNotMatch() {
        // given
        User owner = createUser("005");
        User sharedUser = createUser("006");
        User otherUser = createUser("007");

        Schedule schedule = createSchedule(owner, "공유 일정");

        createShare(schedule, otherUser);

        entityManager.flush();

        // when
        var result = scheduleShareRepository.findRelation(
                schedule.getId(),
                sharedUser.getId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // --------------------------------------------------
    // findSharedSchedules
    // --------------------------------------------------

    @Test
    @DisplayName("특정 사용자가 공유받은 ACTIVE 상태의 일정을 조회한다")
    void findSharedSchedules() {
        // given
        User owner1 = createUser("008");
        User owner2 = createUser("009");
        User sharedUser = createUser("010");
        User otherUser = createUser("011");

        Schedule schedule1 = createSchedule(owner1, "첫 번째 공유 일정");
        Schedule schedule2 = createSchedule(owner2, "두 번째 공유 일정");

        ScheduleShare activeShare1 = createShare(schedule1, sharedUser);
        ScheduleShare activeShare2 = createShare(schedule2, sharedUser);

        ScheduleShare otherUserShare = createShare(schedule1, otherUser);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ScheduleShare> result =
                scheduleShareRepository.findSharedSchedules(
                        sharedUser.getId(),
                        ScheduleShareStatus.ACTIVE
                );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ScheduleShare::getId)
                .containsExactlyInAnyOrder(
                        activeShare1.getId(),
                        activeShare2.getId()
                );

        assertThat(result)
                .extracting(share -> share.getSharedUser().getId())
                .containsOnly(sharedUser.getId());

        // Fetch Join 검증
        for (ScheduleShare share : result) {
            assertThat(Hibernate.isInitialized(share.getSchedule()))
                    .isTrue();

            assertThat(Hibernate.isInitialized(
                    share.getSchedule().getUser()
            )).isTrue();
        }
    }

    @Test
    @DisplayName("공유 상태가 요청한 상태와 다르면 공유 일정 조회에서 제외된다")
    void findSharedSchedulesWhenStatusNotMatch() {
        // given
        User owner = createUser("012");
        User sharedUser = createUser("013");

        Schedule schedule = createSchedule(owner, "취소된 공유 일정");

        ScheduleShare canceledShare = createShare(schedule, sharedUser);
        canceledShare.cancelShare();

        entityManager.flush();

        // when
        List<ScheduleShare> activeResult =
                scheduleShareRepository.findSharedSchedules(
                        sharedUser.getId(),
                        ScheduleShareStatus.ACTIVE
                );

        List<ScheduleShare> canceledResult =
                scheduleShareRepository.findSharedSchedules(
                        sharedUser.getId(),
                        ScheduleShareStatus.CANCELED
                );

        // then
        assertThat(activeResult).isEmpty();

        assertThat(canceledResult)
                .hasSize(1)
                .extracting(ScheduleShare::getId)
                .containsExactly(canceledShare.getId());
    }

    // --------------------------------------------------
    // findByIdWithSchedule
    // --------------------------------------------------

    @Test
    @DisplayName("공유 ID로 일정과 일정 소유자를 함께 조회한다")
    void findByIdWithSchedule() {
        // given
        User owner = createUser("014");
        User sharedUser = createUser("015");

        Schedule schedule = createSchedule(owner, "공유 일정");

        ScheduleShare savedShare = createShare(schedule, sharedUser);

        entityManager.flush();
        entityManager.clear();

        // when
        var result = scheduleShareRepository.findByIdWithSchedule(
                savedShare.getId()
        );

        // then
        assertThat(result).isPresent();

        ScheduleShare share = result.get();

        assertThat(share.getId())
                .isEqualTo(savedShare.getId());

        assertThat(Hibernate.isInitialized(share.getSchedule()))
                .isTrue();

        assertThat(Hibernate.isInitialized(share.getSchedule().getUser()))
                .isTrue();

        assertThat(share.getSchedule().getId())
                .isEqualTo(schedule.getId());

        assertThat(share.getSchedule().getUser().getId())
                .isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("존재하지 않는 공유 ID로 조회하면 빈 Optional을 반환한다")
    void findByIdWithScheduleWhenNotExists() {
        // given
        Long invalidShareId = 999999L;

        // when
        var result = scheduleShareRepository.findByIdWithSchedule(
                invalidShareId
        );

        // then
        assertThat(result).isEmpty();
    }

    // --------------------------------------------------
    // findActiveShareDetail
    // --------------------------------------------------

    @Test
    @DisplayName("공유 ID와 ACTIVE 상태로 공유 상세 정보를 조회한다")
    void findActiveShareDetail() {
        // given
        User owner = createUser("016");
        User sharedUser = createUser("017");

        Schedule schedule = createSchedule(owner, "공유 상세 일정");

        ScheduleShare savedShare = createShare(schedule, sharedUser);

        entityManager.flush();
        entityManager.clear();

        // when
        var result = scheduleShareRepository.findActiveShareDetail(
                savedShare.getId(),
                ScheduleShareStatus.ACTIVE
        );

        // then
        assertThat(result).isPresent();

        assertThat(result.get().getId())
                .isEqualTo(savedShare.getId());

        assertThat(result.get().getScheduleShareStatus())
                .isEqualTo(ScheduleShareStatus.ACTIVE);

        assertThat(Hibernate.isInitialized(result.get().getSchedule()))
                .isTrue();

        assertThat(Hibernate.isInitialized(
                result.get().getSchedule().getUser()
        )).isTrue();
    }

    @Test
    @DisplayName("공유 상태가 요청한 상태와 다르면 상세 정보를 조회하지 않는다")
    void findActiveShareDetailWhenStatusNotMatch() {
        // given
        User owner = createUser("018");
        User sharedUser = createUser("019");

        Schedule schedule = createSchedule(owner, "취소된 공유 일정");

        ScheduleShare share = createShare(schedule, sharedUser);
        share.cancelShare();

        entityManager.flush();

        // when
        var activeResult = scheduleShareRepository.findActiveShareDetail(
                share.getId(),
                ScheduleShareStatus.ACTIVE
        );

        var canceledResult = scheduleShareRepository.findActiveShareDetail(
                share.getId(),
                ScheduleShareStatus.CANCELED
        );

        // then
        assertThat(activeResult).isEmpty();

        assertThat(canceledResult)
                .isPresent()
                .get()
                .extracting(ScheduleShare::getId)
                .isEqualTo(share.getId());
    }

    // --------------------------------------------------
    // findOwnedShares
    // --------------------------------------------------

    @Test
    @DisplayName("일정 소유자가 생성한 ACTIVE 상태의 공유 목록을 조회한다")
    void findOwnedShares() {
        // given
        User owner = createUser("020");
        User otherOwner = createUser("021");
        User sharedUser1 = createUser("022");
        User sharedUser2 = createUser("023");

        Schedule schedule1 = createSchedule(owner, "첫 번째 일정");
        Schedule schedule2 = createSchedule(owner, "두 번째 일정");
        Schedule otherSchedule = createSchedule(otherOwner, "다른 소유자의 일정");

        ScheduleShare share1 = createShare(schedule1, sharedUser1);
        ScheduleShare share2 = createShare(schedule2, sharedUser2);

        ScheduleShare otherShare = createShare(otherSchedule, sharedUser1);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ScheduleShare> result =
                scheduleShareRepository.findOwnedShares(
                        owner.getId(),
                        ScheduleShareStatus.ACTIVE
                );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ScheduleShare::getId)
                .containsExactlyInAnyOrder(
                        share1.getId(),
                        share2.getId()
                );

        assertThat(result)
                .extracting(share -> share.getSchedule().getUser().getId())
                .containsOnly(owner.getId());

        // Fetch Join 검증
        for (ScheduleShare share : result) {
            assertThat(Hibernate.isInitialized(share.getSchedule()))
                    .isTrue();

            assertThat(Hibernate.isInitialized(share.getSharedUser()))
                    .isTrue();
        }
    }

    @Test
    @DisplayName("요청한 공유 상태와 다른 공유 관계는 소유자 공유 목록에서 제외된다")
    void findOwnedSharesWhenStatusNotMatch() {
        // given
        User owner = createUser("024");
        User sharedUser = createUser("025");

        Schedule schedule = createSchedule(owner, "취소된 공유 일정");

        ScheduleShare share = createShare(schedule, sharedUser);
        share.cancelShare();

        entityManager.flush();

        // when
        List<ScheduleShare> activeResult =
                scheduleShareRepository.findOwnedShares(
                        owner.getId(),
                        ScheduleShareStatus.ACTIVE
                );

        List<ScheduleShare> canceledResult =
                scheduleShareRepository.findOwnedShares(
                        owner.getId(),
                        ScheduleShareStatus.CANCELED
                );

        // then
        assertThat(activeResult).isEmpty();

        assertThat(canceledResult)
                .hasSize(1)
                .extracting(ScheduleShare::getId)
                .containsExactly(share.getId());
    }

    // --------------------------------------------------
    // findOwnedShareDetail
    // --------------------------------------------------

    @Test
    @DisplayName("공유 ID와 ACTIVE 상태로 소유자 공유 상세 정보를 조회한다")
    void findOwnedShareDetail() {
        // given
        User owner = createUser("026");
        User sharedUser = createUser("027");

        Schedule schedule = createSchedule(owner, "소유자 공유 상세 일정");

        ScheduleShare savedShare = createShare(schedule, sharedUser);

        entityManager.flush();
        entityManager.clear();

        // when
        var result = scheduleShareRepository.findOwnedShareDetail(
                savedShare.getId(),
                ScheduleShareStatus.ACTIVE
        );

        // then
        assertThat(result).isPresent();

        ScheduleShare share = result.get();

        assertThat(share.getId())
                .isEqualTo(savedShare.getId());

        assertThat(Hibernate.isInitialized(share.getSchedule()))
                .isTrue();

        assertThat(Hibernate.isInitialized(share.getSchedule().getUser()))
                .isTrue();

        assertThat(Hibernate.isInitialized(share.getSharedUser()))
                .isTrue();

        assertThat(share.getSchedule().getUser().getId())
                .isEqualTo(owner.getId());

        assertThat(share.getSharedUser().getId())
                .isEqualTo(sharedUser.getId());
    }

    @Test
    @DisplayName("요청한 상태와 다른 공유 관계는 소유자 공유 상세 정보에서 조회되지 않는다")
    void findOwnedShareDetailWhenStatusNotMatch() {
        // given
        User owner = createUser("028");
        User sharedUser = createUser("029");

        Schedule schedule = createSchedule(owner, "취소된 공유 일정");

        ScheduleShare share = createShare(schedule, sharedUser);
        share.cancelShare();

        entityManager.flush();

        // when
        var activeResult = scheduleShareRepository.findOwnedShareDetail(
                share.getId(),
                ScheduleShareStatus.ACTIVE
        );

        var canceledResult = scheduleShareRepository.findOwnedShareDetail(
                share.getId(),
                ScheduleShareStatus.CANCELED
        );

        // then
        assertThat(activeResult).isEmpty();

        assertThat(canceledResult)
                .isPresent()
                .get()
                .extracting(ScheduleShare::getId)
                .isEqualTo(share.getId());
    }

    // --------------------------------------------------
    // findActiveRelation
    // --------------------------------------------------

    @Test
    @DisplayName("일정 ID와 사용자 ID 및 ACTIVE 상태로 공유 관계를 조회한다")
    void findActiveRelation() {
        // given
        User owner = createUser("030");
        User sharedUser = createUser("031");

        Schedule schedule = createSchedule(owner, "활성 공유 일정");

        ScheduleShare savedShare = createShare(schedule, sharedUser);

        entityManager.flush();
        entityManager.clear();

        // when
        var result = scheduleShareRepository.findActiveRelation(
                schedule.getId(),
                sharedUser.getId(),
                ScheduleShareStatus.ACTIVE
        );

        // then
        assertThat(result).isPresent();

        assertThat(result.get().getId())
                .isEqualTo(savedShare.getId());

        assertThat(result.get().getScheduleShareStatus())
                .isEqualTo(ScheduleShareStatus.ACTIVE);
    }

    @Test
    @DisplayName("CANCELED 상태의 공유 관계는 활성 공유 관계로 조회되지 않는다")
    void findActiveRelationWhenCanceled() {
        // given
        User owner = createUser("032");
        User sharedUser = createUser("033");

        Schedule schedule = createSchedule(owner, "취소된 공유 일정");

        ScheduleShare share = createShare(schedule, sharedUser);
        share.cancelShare();

        entityManager.flush();

        // when
        var activeResult = scheduleShareRepository.findActiveRelation(
                schedule.getId(),
                sharedUser.getId(),
                ScheduleShareStatus.ACTIVE
        );

        var canceledResult = scheduleShareRepository.findActiveRelation(
                schedule.getId(),
                sharedUser.getId(),
                ScheduleShareStatus.CANCELED
        );

        // then
        assertThat(activeResult).isEmpty();

        assertThat(canceledResult)
                .isPresent()
                .get()
                .extracting(ScheduleShare::getId)
                .isEqualTo(share.getId());
    }

    // --------------------------------------------------
    // existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus
    // --------------------------------------------------

    @Test
    @DisplayName("일정 ID와 사용자 ID 및 공유 상태로 공유 여부를 확인한다")
    void existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus() {
        // given
        User owner = createUser("034");
        User sharedUser = createUser("035");
        User otherUser = createUser("036");

        Schedule schedule = createSchedule(owner, "공유 여부 확인 일정");
        Schedule otherSchedule = createSchedule(owner, "다른 일정");

        createShare(schedule, sharedUser);
        createShare(otherSchedule, otherUser);

        entityManager.flush();

        // when
        boolean exists =
                scheduleShareRepository
                        .existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                                schedule.getId(),
                                sharedUser.getId(),
                                ScheduleShareStatus.ACTIVE
                        );

        boolean notExists =
                scheduleShareRepository
                        .existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                                schedule.getId(),
                                otherUser.getId(),
                                ScheduleShareStatus.ACTIVE
                        );

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("CANCELED 상태의 공유 관계는 ACTIVE 상태로 존재하지 않는다")
    void existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatusWhenCanceled() {
        // given
        User owner = createUser("037");
        User sharedUser = createUser("038");

        Schedule schedule = createSchedule(owner, "취소된 공유 일정");

        ScheduleShare share = createShare(schedule, sharedUser);
        share.cancelShare();

        entityManager.flush();

        // when
        boolean activeExists =
                scheduleShareRepository
                        .existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                                schedule.getId(),
                                sharedUser.getId(),
                                ScheduleShareStatus.ACTIVE
                        );

        boolean canceledExists =
                scheduleShareRepository
                        .existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                                schedule.getId(),
                                sharedUser.getId(),
                                ScheduleShareStatus.CANCELED
                        );

        // then
        assertThat(activeExists).isFalse();
        assertThat(canceledExists).isTrue();
    }

    // --------------------------------------------------
    // findAllBySchedule_Id
    // --------------------------------------------------

    @Test
    @DisplayName("특정 일정에 등록된 모든 공유 관계를 조회한다")
    void findAllBySchedule_Id() {
        // given
        User owner = createUser("039");
        User sharedUser1 = createUser("040");
        User sharedUser2 = createUser("041");
        User otherUser = createUser("042");

        Schedule schedule = createSchedule(owner, "공유 관계 조회 일정");
        Schedule otherSchedule = createSchedule(owner, "다른 일정");

        ScheduleShare share1 = createShare(schedule, sharedUser1);
        ScheduleShare share2 = createShare(schedule, sharedUser2);

        createShare(otherSchedule, otherUser);

        entityManager.flush();

        // when
        List<ScheduleShare> result =
                scheduleShareRepository.findAllBySchedule_Id(
                        schedule.getId()
                );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ScheduleShare::getId)
                .containsExactlyInAnyOrder(
                        share1.getId(),
                        share2.getId()
                );
    }

    // --------------------------------------------------
    // deleteAllBySharedUserId
    // --------------------------------------------------

    @Test
    @DisplayName("특정 사용자에게 공유된 모든 공유 관계를 삭제한다")
    void deleteAllBySharedUserId() {
        // given
        User owner1 = createUser("043");
        User owner2 = createUser("044");
        User sharedUser = createUser("045");
        User otherUser = createUser("046");

        Schedule schedule1 = createSchedule(owner1, "첫 번째 일정");
        Schedule schedule2 = createSchedule(owner2, "두 번째 일정");

        ScheduleShare target1 = createShare(schedule1, sharedUser);
        ScheduleShare target2 = createShare(schedule2, sharedUser);

        ScheduleShare otherShare = createShare(schedule1, otherUser);

        entityManager.flush();

        // when
        scheduleShareRepository.deleteAllBySharedUserId(
                sharedUser.getId()
        );

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(scheduleShareRepository.findById(target1.getId()))
                .isEmpty();

        assertThat(scheduleShareRepository.findById(target2.getId()))
                .isEmpty();

        assertThat(scheduleShareRepository.findById(otherShare.getId()))
                .isPresent();
    }

    // --------------------------------------------------
    // softDeleteOwnedShares
    // --------------------------------------------------

    @Test
    @DisplayName("특정 사용자가 소유한 일정의 공유 관계를 Soft Delete 처리한다")
    void softDeleteOwnedShares() {
        // given
        User owner = createUser("047");
        User otherOwner = createUser("048");
        User sharedUser = createUser("049");

        Schedule ownedSchedule1 = createSchedule(owner, "첫 번째 소유 일정");
        Schedule ownedSchedule2 = createSchedule(owner, "두 번째 소유 일정");
        Schedule otherSchedule = createSchedule(otherOwner, "다른 소유자의 일정");

        ScheduleShare ownedShare1 =
                createShare(ownedSchedule1, sharedUser);

        ScheduleShare ownedShare2 =
                createShare(ownedSchedule2, sharedUser);

        ScheduleShare otherShare =
                createShare(otherSchedule, sharedUser);

        entityManager.flush();

        // when
        scheduleShareRepository.softDeleteOwnedShares(
                owner.getId()
        );

        entityManager.flush();
        entityManager.clear();

        // then
        ScheduleShare deletedShare1 =
                scheduleShareRepository.findById(ownedShare1.getId())
                        .orElseThrow();

        ScheduleShare deletedShare2 =
                scheduleShareRepository.findById(ownedShare2.getId())
                        .orElseThrow();

        ScheduleShare notDeletedShare =
                scheduleShareRepository.findById(otherShare.getId())
                        .orElseThrow();

        // 첫 번째 소유 일정의 공유 관계
        assertThat(deletedShare1.getScheduleShareStatus())
                .isEqualTo(ScheduleShareStatus.SCHEDULE_DELETED);

        assertThat(deletedShare1.getDeletedAt()).isNotNull();
        assertThat(deletedShare1.isDeleted()).isTrue();

        // 두 번째 소유 일정의 공유 관계
        assertThat(deletedShare2.getScheduleShareStatus())
                .isEqualTo(ScheduleShareStatus.SCHEDULE_DELETED);

        assertThat(deletedShare2.getDeletedAt()).isNotNull();
        assertThat(deletedShare2.isDeleted()).isTrue();

        // 다른 사용자가 소유한 일정의 공유 관계
        assertThat(notDeletedShare.getScheduleShareStatus())
                .isEqualTo(ScheduleShareStatus.ACTIVE);

        assertThat(notDeletedShare.getDeletedAt()).isNull();
        assertThat(notDeletedShare.isDeleted()).isFalse();
    }
}