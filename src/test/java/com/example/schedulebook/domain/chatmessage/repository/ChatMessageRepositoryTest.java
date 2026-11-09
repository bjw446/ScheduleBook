package com.example.schedulebook.domain.chatmessage.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatmessage.enums.SystemMessageType;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
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
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private User createUser(String suffix) {
        return User.create(
                "login" + suffix,
                "password",
                "nickname" + suffix,
                "email" + suffix + "@test.com",
                "0101234" + suffix
        );
    }

    private ChatRoom createChatRoom() {
        ChatRoom chatRoom = ChatRoom.direct();
        entityManager.persist(chatRoom);
        return chatRoom;
    }

    private ChatMessage createMessage(
            ChatRoom chatRoom,
            User sender,
            String content
    ) {
        ChatMessage message = ChatMessage.of(
                chatRoom,
                sender,
                content,
                ChatMessageType.TEXT,
                null
        );

        entityManager.persist(message);

        return message;
    }

    private Schedule createSchedule(User user, String suffix) {
        Schedule schedule = Schedule.create(
                user,
                "일정 제목" + suffix,
                "일정 내용" + suffix,
                LocalDate.of(2026, 12, 1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        entityManager.persist(schedule);

        return schedule;
    }

    @Test
    @DisplayName("메시지 ID와 채팅방 ID로 메시지를 조회한다")
    void findByIdAndChatRoomId() {
        // given
        User sender = createUser("01");
        entityManager.persist(sender);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage message = createMessage(
                chatRoom,
                sender,
                "안녕하세요."
        );

        entityManager.flush();
        entityManager.clear();

        // when
        ChatMessage foundMessage = chatMessageRepository
                .findByIdAndChatRoomId(message.getId(), chatRoom.getId())
                .orElseThrow();

        // then
        assertThat(foundMessage.getId()).isEqualTo(message.getId());
        assertThat(foundMessage.getChatRoom().getId()).isEqualTo(chatRoom.getId());
        assertThat(foundMessage.getContent()).isEqualTo("안녕하세요.");
    }

    @Test
    @DisplayName("다른 채팅방의 메시지는 조회되지 않는다")
    void findByIdAndChatRoomIdWhenRoomIdDifferent() {
        // given
        User sender = createUser("02");
        entityManager.persist(sender);

        ChatRoom firstRoom = createChatRoom();
        ChatRoom secondRoom = createChatRoom();

        ChatMessage message = createMessage(
                firstRoom,
                sender,
                "첫 번째 채팅방 메시지"
        );

        entityManager.flush();
        entityManager.clear();

        // when
        var result = chatMessageRepository.findByIdAndChatRoomId(
                message.getId(),
                secondRoom.getId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 메시지 ID로 조회하면 빈 Optional을 반환한다")
    void findByIdAndChatRoomIdWhenMessageNotFound() {
        // given
        ChatRoom chatRoom = createChatRoom();

        entityManager.flush();
        entityManager.clear();

        // when
        var result = chatMessageRepository.findByIdAndChatRoomId(
                99999L,
                chatRoom.getId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("채팅방의 참여 시점 이후 메시지를 ID 내림차순으로 조회한다")
    void findMessages() {
        // given
        User sender = createUser("03");
        entityManager.persist(sender);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage firstMessage = createMessage(
                chatRoom,
                sender,
                "첫 번째 메시지"
        );

        ChatMessage secondMessage = createMessage(
                chatRoom,
                sender,
                "두 번째 메시지"
        );

        ChatMessage thirdMessage = createMessage(
                chatRoom,
                sender,
                "세 번째 메시지"
        );

        LocalDateTime joinedAt = LocalDateTime.now().minusDays(1);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatMessage> result = chatMessageRepository.findMessages(
                chatRoom.getId(),
                joinedAt,
                null,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result)
                .hasSize(3)
                .extracting(ChatMessage::getId)
                .containsExactly(
                        thirdMessage.getId(),
                        secondMessage.getId(),
                        firstMessage.getId()
                );
    }

    @Test
    @DisplayName("채팅방 참여 시점 이전에 작성된 메시지는 조회하지 않는다")
    void findMessagesWhenMessageCreatedBeforeJoinedAt() {
        // given
        User sender = createUser("04");
        entityManager.persist(sender);

        ChatRoom chatRoom = createChatRoom();

        createMessage(
                chatRoom,
                sender,
                "참여 이전 메시지"
        );

        LocalDateTime joinedAt = LocalDateTime.now().plusDays(1);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatMessage> result = chatMessageRepository.findMessages(
                chatRoom.getId(),
                joinedAt,
                null,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("커서보다 작은 메시지 ID만 조회한다")
    void findMessagesWithCursor() {
        // given
        User sender = createUser("05");
        entityManager.persist(sender);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage firstMessage = createMessage(
                chatRoom,
                sender,
                "첫 번째 메시지"
        );

        ChatMessage secondMessage = createMessage(
                chatRoom,
                sender,
                "두 번째 메시지"
        );

        ChatMessage thirdMessage = createMessage(
                chatRoom,
                sender,
                "세 번째 메시지"
        );

        LocalDateTime joinedAt = LocalDateTime.now().minusDays(1);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatMessage> result = chatMessageRepository.findMessages(
                chatRoom.getId(),
                joinedAt,
                thirdMessage.getId(),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ChatMessage::getId)
                .containsExactly(
                        secondMessage.getId(),
                        firstMessage.getId()
                );
    }

    @Test
    @DisplayName("페이지 크기에 맞춰 메시지를 조회한다")
    void findMessagesWithPageable() {
        // given
        User sender = createUser("06");
        entityManager.persist(sender);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage firstMessage = createMessage(
                chatRoom,
                sender,
                "첫 번째 메시지"
        );

        ChatMessage secondMessage = createMessage(
                chatRoom,
                sender,
                "두 번째 메시지"
        );

        ChatMessage thirdMessage = createMessage(
                chatRoom,
                sender,
                "세 번째 메시지"
        );

        LocalDateTime joinedAt = LocalDateTime.now().minusDays(1);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatMessage> result = chatMessageRepository.findMessages(
                chatRoom.getId(),
                joinedAt,
                null,
                PageRequest.of(0, 2)
        );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ChatMessage::getId)
                .containsExactly(
                        thirdMessage.getId(),
                        secondMessage.getId()
                );
    }

    @Test
    @DisplayName("답장 메시지와 답장 작성자 정보를 함께 조회한다")
    void findMessagesWithReplyMessage() {
        // given
        User sender = createUser("07");
        User replySender = createUser("08");

        entityManager.persist(sender);
        entityManager.persist(replySender);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage originalMessage = createMessage(
                chatRoom,
                replySender,
                "원본 메시지"
        );

        ChatMessage replyMessage = ChatMessage.of(
                chatRoom,
                sender,
                "답장 메시지",
                ChatMessageType.TEXT,
                originalMessage
        );

        entityManager.persist(replyMessage);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatMessage> result = chatMessageRepository.findMessages(
                chatRoom.getId(),
                LocalDateTime.now().minusDays(1),
                null,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).hasSize(2);

        ChatMessage foundReplyMessage = result.get(0);

        PersistenceUnitUtil persistenceUnitUtil =
                entityManagerFactory.getPersistenceUnitUtil();

        assertThat(foundReplyMessage.getContent()).isEqualTo("답장 메시지");

        assertThat(persistenceUnitUtil.isLoaded(foundReplyMessage, "replyMessage"))
                .isTrue();

        ChatMessage foundOriginalMessage = foundReplyMessage.getReplyMessage();

        assertThat(persistenceUnitUtil.isLoaded(foundOriginalMessage, "sender"))
                .isTrue();

        assertThat(foundOriginalMessage.getId())
                .isEqualTo(originalMessage.getId());

        assertThat(foundOriginalMessage.getSender().getId())
                .isEqualTo(replySender.getId());
    }

    @Test
    @DisplayName("마지막 읽은 메시지 이후 상대방이 보낸 메시지 개수를 조회한다")
    void countUnreadMessages() {
        // given
        User currentUser = createUser("09");
        User opponent = createUser("10");

        entityManager.persist(currentUser);
        entityManager.persist(opponent);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage lastReadMessage = createMessage(
                chatRoom,
                currentUser,
                "마지막으로 읽은 메시지"
        );

        createMessage(
                chatRoom,
                opponent,
                "상대방의 첫 번째 메시지"
        );

        createMessage(
                chatRoom,
                opponent,
                "상대방의 두 번째 메시지"
        );

        createMessage(
                chatRoom,
                currentUser,
                "내가 보낸 메시지"
        );

        LocalDateTime joinedAt = LocalDateTime.now().minusDays(1);

        entityManager.flush();
        entityManager.clear();

        // when
        long result = chatMessageRepository.countUnreadMessages(
                chatRoom.getId(),
                lastReadMessage.getId(),
                currentUser.getId(),
                joinedAt
        );

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("발신자가 없는 시스템 메시지도 안 읽은 메시지에 포함한다")
    void countUnreadMessagesIncludingSystemMessage() {
        // given
        User currentUser = createUser("11");
        User opponent = createUser("12");

        entityManager.persist(currentUser);
        entityManager.persist(opponent);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage lastReadMessage = createMessage(
                chatRoom,
                currentUser,
                "마지막으로 읽은 메시지"
        );

        createMessage(
                chatRoom,
                opponent,
                "상대방 메시지"
        );

        ChatMessage systemMessage = ChatMessage.system(
                chatRoom,
                SystemMessageType.USER_JOIN,
                "nickname12님이 채팅방에 입장했습니다."
        );

        entityManager.persist(systemMessage);

        LocalDateTime joinedAt = LocalDateTime.now().minusDays(1);

        entityManager.flush();
        entityManager.clear();

        // when
        long result = chatMessageRepository.countUnreadMessages(
                chatRoom.getId(),
                lastReadMessage.getId(),
                currentUser.getId(),
                joinedAt
        );

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("채팅방 참여 시점 이전의 메시지는 안 읽은 메시지 개수에 포함하지 않는다")
    void countUnreadMessagesWhenMessageCreatedBeforeJoinedAt() {
        // given
        User currentUser = createUser("13");
        User opponent = createUser("14");

        entityManager.persist(currentUser);
        entityManager.persist(opponent);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage lastReadMessage = createMessage(
                chatRoom,
                currentUser,
                "마지막으로 읽은 메시지"
        );

        createMessage(
                chatRoom,
                opponent,
                "상대방 메시지"
        );

        LocalDateTime joinedAt = LocalDateTime.now().plusDays(1);

        entityManager.flush();
        entityManager.clear();

        // when
        long result = chatMessageRepository.countUnreadMessages(
                chatRoom.getId(),
                lastReadMessage.getId(),
                currentUser.getId(),
                joinedAt
        );

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("마지막 읽은 메시지 ID보다 큰 메시지만 안 읽은 메시지로 집계한다")
    void countUnreadMessagesAfterLastReadMessage() {
        // given
        User currentUser = createUser("15");
        User opponent = createUser("16");

        entityManager.persist(currentUser);
        entityManager.persist(opponent);

        ChatRoom chatRoom = createChatRoom();

        createMessage(
                chatRoom,
                opponent,
                "읽은 메시지"
        );

        ChatMessage lastReadMessage = createMessage(
                chatRoom,
                currentUser,
                "마지막 읽은 메시지"
        );

        createMessage(
                chatRoom,
                opponent,
                "읽지 않은 메시지"
        );

        LocalDateTime joinedAt = LocalDateTime.now().minusDays(1);

        entityManager.flush();
        entityManager.clear();

        // when
        long result = chatMessageRepository.countUnreadMessages(
                chatRoom.getId(),
                lastReadMessage.getId(),
                currentUser.getId(),
                joinedAt
        );

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("일정 ID로 삭제되지 않은 메시지를 조회한다")
    void findAllByScheduleIdAndDeletedFalse() {
        // given
        User user = createUser("17");
        entityManager.persist(user);

        ChatRoom chatRoom = createChatRoom();

        Schedule firstSchedule = createSchedule(user, "01");
        Schedule secondSchedule = createSchedule(user, "02");

        ChatMessage firstMessage = ChatMessage.schedule(
                chatRoom,
                user,
                firstSchedule
        );

        ChatMessage secondMessage = ChatMessage.schedule(
                chatRoom,
                user,
                firstSchedule
        );

        ChatMessage anotherScheduleMessage = ChatMessage.schedule(
                chatRoom,
                user,
                secondSchedule
        );

        entityManager.persist(firstMessage);
        entityManager.persist(secondMessage);
        entityManager.persist(anotherScheduleMessage);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatMessage> result =
                chatMessageRepository.findAllByScheduleIdAndDeletedFalse(
                        firstSchedule.getId()
                );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ChatMessage::getId)
                .containsExactlyInAnyOrder(
                        firstMessage.getId(),
                        secondMessage.getId()
                );
    }

    @Test
    @DisplayName("삭제된 일정 메시지는 조회 결과에 포함하지 않는다")
    void findAllByScheduleIdAndDeletedFalseWhenMessageDeleted() {
        // given
        User user = createUser("18");
        entityManager.persist(user);

        ChatRoom chatRoom = createChatRoom();
        Schedule schedule = createSchedule(user, "03");

        ChatMessage activeMessage = ChatMessage.schedule(
                chatRoom,
                user,
                schedule
        );

        ChatMessage deletedMessage = ChatMessage.schedule(
                chatRoom,
                user,
                schedule
        );

        entityManager.persist(activeMessage);
        entityManager.persist(deletedMessage);

        deletedMessage.deleteMessage(user.getId());

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatMessage> result =
                chatMessageRepository.findAllByScheduleIdAndDeletedFalse(
                        schedule.getId()
                );

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(ChatMessage::getId)
                .containsExactly(activeMessage.getId());
    }

    @Test
    @DisplayName("존재하지 않는 일정 ID로 조회하면 빈 목록을 반환한다")
    void findAllByScheduleIdAndDeletedFalseWhenScheduleNotFound() {
        // given
        // when
        List<ChatMessage> result =
                chatMessageRepository.findAllByScheduleIdAndDeletedFalse(99999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("메시지 ID로 채팅방 정보를 함께 조회한다")
    void findByIdWithChatRoom() {
        // given
        User sender = createUser("19");
        entityManager.persist(sender);

        ChatRoom chatRoom = createChatRoom();

        ChatMessage message = createMessage(
                chatRoom,
                sender,
                "채팅방 정보를 함께 조회할 메시지"
        );

        entityManager.flush();
        entityManager.clear();

        // when
        ChatMessage foundMessage = chatMessageRepository
                .findByIdWithChatRoom(message.getId())
                .orElseThrow();

        // then
        PersistenceUnitUtil persistenceUnitUtil =
                entityManagerFactory.getPersistenceUnitUtil();

        assertThat(foundMessage.getId()).isEqualTo(message.getId());

        assertThat(persistenceUnitUtil.isLoaded(foundMessage, "chatRoom"))
                .isTrue();

        assertThat(foundMessage.getChatRoom().getId())
                .isEqualTo(chatRoom.getId());
    }

    @Test
    @DisplayName("존재하지 않는 메시지 ID로 조회하면 빈 Optional을 반환한다")
    void findByIdWithChatRoomWhenMessageNotFound() {
        // given
        // when
        var result = chatMessageRepository.findByIdWithChatRoom(99999L);

        // then
        assertThat(result).isEmpty();
    }
}