package com.example.schedulebook.domain.schedulesnapshot.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomRepository;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshotHistory;
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
class ScheduleSnapshotHistoryRepositoryTest {

    @Autowired
    private ScheduleSnapshotHistoryRepository scheduleSnapshotHistoryRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

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

    private ChatRoom createChatRoom() {
        return chatRoomRepository.save(ChatRoom.direct());
    }

    private ChatMessage createScheduleMessage(
            ChatRoom chatRoom,
            User sender,
            Schedule schedule
    ) {
        return entityManager.merge(
                ChatMessage.schedule(chatRoom, sender, schedule)
        );
    }

    private ScheduleSnapshotHistory createHistory(ChatMessage chatMessage) {
        return scheduleSnapshotHistoryRepository.save(
                ScheduleSnapshotHistory.of(chatMessage)
        );
    }

    private void updateScheduleVersion(Schedule schedule, Long version) {
        while (schedule.getScheduleVersion() < version) {
            schedule.update(
                    schedule.getTitle(),
                    schedule.getContent(),
                    schedule.getScheduleDate(),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0)
            );
        }
    }

    // --------------------------------------------------
    // findAllByChatMessageId
    // --------------------------------------------------

    @Test
    @DisplayName("특정 채팅 메시지 ID로 Snapshot History 목록을 조회한다")
    void findAllByChatMessageId() {
        // given
        User owner = createUser("001");
        ChatRoom chatRoom = createChatRoom();

        Schedule schedule = createSchedule(owner, "공유 일정");

        ChatMessage chatMessage1 =
                createScheduleMessage(chatRoom, owner, schedule);

        ChatMessage chatMessage2 =
                createScheduleMessage(chatRoom, owner, schedule);

        ScheduleSnapshotHistory history1 = createHistory(chatMessage1);
        ScheduleSnapshotHistory history2 = createHistory(chatMessage1);

        createHistory(chatMessage2);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ScheduleSnapshotHistory> result =
                scheduleSnapshotHistoryRepository.findAllByChatMessageId(
                        chatMessage1.getId()
                );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ScheduleSnapshotHistory::getId)
                .containsExactly(
                        history2.getId(),
                        history1.getId()
                );

        assertThat(result)
                .extracting(history -> history.getChatMessage().getId())
                .containsOnly(chatMessage1.getId());
    }

    @Test
    @DisplayName("Snapshot 버전이 높은 순서로 History를 정렬한다")
    void findAllByChatMessageIdOrderByVersionDesc() {
        // given
        User owner = createUser("002");
        ChatRoom chatRoom = createChatRoom();

        // 버전 1 Schedule
        Schedule scheduleV1 = createSchedule(owner, "버전 1 일정");

        // 버전 3 Schedule
        Schedule scheduleV3 = createSchedule(owner, "버전 3 일정");
        updateScheduleVersion(scheduleV3, 3L);

        // 버전 2 Schedule
        Schedule scheduleV2 = createSchedule(owner, "버전 2 일정");
        updateScheduleVersion(scheduleV2, 2L);

        // 하나의 ChatMessage 생성
        ChatMessage chatMessage =
                createScheduleMessage(chatRoom, owner, scheduleV1);

        // 버전 1 → 3 → 2 순서로 이력 저장
        ScheduleSnapshotHistory history1 = createHistory(chatMessage);

        chatMessage.updateScheduleSnapshot(scheduleV3);
        ScheduleSnapshotHistory history3 = createHistory(chatMessage);

        chatMessage.updateScheduleSnapshot(scheduleV2);
        ScheduleSnapshotHistory history2 = createHistory(chatMessage);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ScheduleSnapshotHistory> result =
                scheduleSnapshotHistoryRepository.findAllByChatMessageId(
                        chatMessage.getId()
                );

        // then
        assertThat(result)
                .hasSize(3)
                .extracting(history -> history.getScheduleSnapshot().getScheduleVersion())
                .containsExactly(3L, 2L, 1L);
    }

    @Test
    @DisplayName("Snapshot 버전이 같으면 History ID 내림차순으로 정렬한다")
    void findAllByChatMessageIdOrderByIdDesc() {
        // given
        User owner = createUser("003");
        ChatRoom chatRoom = createChatRoom();

        Schedule schedule = createSchedule(owner, "동일 버전 정렬 일정");

        ChatMessage chatMessage =
                createScheduleMessage(chatRoom, owner, schedule);

        ScheduleSnapshotHistory history1 = createHistory(chatMessage);
        ScheduleSnapshotHistory history2 = createHistory(chatMessage);
        ScheduleSnapshotHistory history3 = createHistory(chatMessage);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ScheduleSnapshotHistory> result =
                scheduleSnapshotHistoryRepository.findAllByChatMessageId(
                        chatMessage.getId()
                );

        // then
        assertThat(result)
                .hasSize(3)
                .extracting(ScheduleSnapshotHistory::getId)
                .containsExactly(
                        history3.getId(),
                        history2.getId(),
                        history1.getId()
                );
    }

    @Test
    @DisplayName("존재하지 않는 채팅 메시지 ID로 조회하면 빈 목록을 반환한다")
    void findAllByChatMessageIdWhenNotExists() {
        // given
        Long invalidMessageId = 999999L;

        // when
        List<ScheduleSnapshotHistory> result =
                scheduleSnapshotHistoryRepository.findAllByChatMessageId(
                        invalidMessageId
                );

        // then
        assertThat(result).isEmpty();
    }

    // --------------------------------------------------
    // findByChatMessageIdAndVersion
    // --------------------------------------------------

    @Test
    @DisplayName("채팅 메시지 ID와 Snapshot 버전으로 History를 조회한다")
    void findByChatMessageIdAndVersion() {
        // given
        User owner = createUser("004");
        ChatRoom chatRoom = createChatRoom();

        Schedule schedule = createSchedule(owner, "버전 조회 일정");

        updateScheduleVersion(schedule, 3L);

        ChatMessage chatMessage =
                createScheduleMessage(chatRoom, owner, schedule);

        ScheduleSnapshotHistory savedHistory =
                createHistory(chatMessage);

        entityManager.flush();
        entityManager.clear();

        // when
        var result =
                scheduleSnapshotHistoryRepository.findByChatMessageIdAndVersion(
                        chatMessage.getId(),
                        3L
                );

        // then
        assertThat(result).isPresent();

        assertThat(result.get().getId())
                .isEqualTo(savedHistory.getId());

        assertThat(result.get().getChatMessage().getId())
                .isEqualTo(chatMessage.getId());

        assertThat(result.get().getScheduleSnapshot().getScheduleVersion())
                .isEqualTo(3L);
    }

    @Test
    @DisplayName("요청한 Snapshot 버전이 다르면 빈 Optional을 반환한다")
    void findByChatMessageIdAndVersionWhenVersionNotMatch() {
        // given
        User owner = createUser("005");
        ChatRoom chatRoom = createChatRoom();

        Schedule schedule = createSchedule(owner, "버전 불일치 일정");

        updateScheduleVersion(schedule, 3L);

        ChatMessage chatMessage =
                createScheduleMessage(chatRoom, owner, schedule);

        createHistory(chatMessage);

        entityManager.flush();

        // when
        var result =
                scheduleSnapshotHistoryRepository.findByChatMessageIdAndVersion(
                        chatMessage.getId(),
                        2L
                );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("다른 채팅 메시지 ID로는 Snapshot History를 조회할 수 없다")
    void findByChatMessageIdAndVersionWhenMessageIdNotMatch() {
        // given
        User owner = createUser("006");
        ChatRoom chatRoom = createChatRoom();

        Schedule schedule = createSchedule(owner, "메시지 ID 불일치 일정");

        ChatMessage chatMessage =
                createScheduleMessage(chatRoom, owner, schedule);

        createHistory(chatMessage);

        ChatMessage otherMessage =
                createScheduleMessage(chatRoom, owner, schedule);

        entityManager.flush();

        // when
        var result =
                scheduleSnapshotHistoryRepository.findByChatMessageIdAndVersion(
                        otherMessage.getId(),
                        1L
                );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 채팅 메시지 ID로 조회하면 빈 Optional을 반환한다")
    void findByChatMessageIdAndVersionWhenMessageNotExists() {
        // given
        Long invalidMessageId = 999999L;

        // when
        var result =
                scheduleSnapshotHistoryRepository.findByChatMessageIdAndVersion(
                        invalidMessageId,
                        1L
                );

        // then
        assertThat(result).isEmpty();
    }
}