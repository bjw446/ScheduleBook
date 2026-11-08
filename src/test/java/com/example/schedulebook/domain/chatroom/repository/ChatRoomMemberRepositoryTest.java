package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.projection.ChatRoomListProjection;
import com.example.schedulebook.domain.chatroom.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chatroom.projection.OpponentInfoProjection;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ChatRoomMemberRepositoryTest {

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private EntityManager entityManager;

    private static final LocalDateTime FIXED_TIME =
            LocalDateTime.of(2026, 11, 8, 12, 0, 0);

    private static final LocalDateTime FIXED_JOINED_AT =
            LocalDateTime.of(2026, 11, 8, 12, 10, 0);

    private static final LocalDateTime FIXED_DELETED_AT =
            LocalDateTime.of(2026, 11, 8, 12, 20, 0);

    private User createUser(String suffix) {
        return User.create(
                "login" + suffix,
                "password",
                "nickname" + suffix,
                "email" + suffix + "@test.com",
                "0101234" + suffix
        );
    }

    private ChatRoom createGroupChatRoom(String name) {
        ChatRoom chatRoom = ChatRoom.group(name);
        entityManager.persist(chatRoom);
        return chatRoom;
    }

    private ChatRoomMember createMember(ChatRoom chatRoom, User user) {
        ChatRoomMember member = ChatRoomMember.of(
                chatRoom,
                user,
                FIXED_TIME
        );

        entityManager.persist(member);
        return member;
    }

    private ChatMessage createMessage(ChatRoom chatRoom, User sender, String content) {
        ChatMessage message = ChatMessage.of(
                chatRoom,
                sender,
                content,
                ChatMessageType.TEXT,
                null
        );

        entityManager.persist(message);
        chatRoom.updateLastMessage(message);

        return message;
    }

    @Test
    @DisplayName("채팅방 ID와 사용자 ID로 활성 멤버의 사용자를 조회한다")
    void findUserInRoom() {
        // given
        User user = createUser("01");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);
        createMember(chatRoom, user);

        // when
        User foundUser = chatRoomMemberRepository
                .findUserInRoom(chatRoom.getId(), user.getId())
                .orElseThrow();

        // then
        assertThat(foundUser.getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("삭제된 채팅방 멤버는 사용자 조회 결과에 포함되지 않는다")
    void findUserInRoomWhenMemberDeleted() {
        // given
        User user = createUser("02");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);

        ChatRoomMember member = createMember(chatRoom, user);
        member.delete();

        entityManager.flush();
        entityManager.clear();

        // when
        var foundUser = chatRoomMemberRepository
                .findUserInRoom(chatRoom.getId(), user.getId());

        // then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("채팅방 ID와 사용자 ID로 활성 멤버를 조회한다")
    void findActiveByChatRoomIdAndUserId() {
        // given
        User user = createUser("03");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);

        ChatRoomMember member = createMember(chatRoom, user);

        // when
        ChatRoomMember foundMember = chatRoomMemberRepository
                .findActiveByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
                .orElseThrow();

        // then
        assertThat(foundMember.getId()).isEqualTo(member.getId());
        assertThat(foundMember.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("삭제된 멤버는 활성 멤버 조회 결과에 포함되지 않는다")
    void findActiveByChatRoomIdAndUserIdWhenMemberDeleted() {
        // given
        User user = createUser("04");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);

        ChatRoomMember member = createMember(chatRoom, user);
        member.delete();

        entityManager.flush();
        entityManager.clear();

        // when
        var foundMember = chatRoomMemberRepository
                .findActiveByChatRoomIdAndUserId(chatRoom.getId(), user.getId());

        // then
        assertThat(foundMember).isEmpty();
    }

    @Test
    @DisplayName("메시지 발신자를 제외한 활성 멤버의 안 읽은 메시지 수를 증가시킨다")
    void increaseUnreadCount() {
        // given
        User sender = createUser("05");
        User receiver = createUser("06");
        User deletedUser = createUser("07");

        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(sender);
        entityManager.persist(receiver);
        entityManager.persist(deletedUser);

        ChatRoomMember senderMember = createMember(chatRoom, sender);
        ChatRoomMember receiverMember = createMember(chatRoom, receiver);
        ChatRoomMember deletedMember = createMember(chatRoom, deletedUser);

        senderMember.updateUnreadCount(2);
        receiverMember.updateUnreadCount(2);
        deletedMember.updateUnreadCount(2);
        deletedMember.delete();

        entityManager.flush();
        entityManager.clear();

        // when
        chatRoomMemberRepository.increaseUnreadCount(
                chatRoom.getId(),
                sender.getId()
        );

        entityManager.flush();
        entityManager.clear();

        // then
        ChatRoomMember foundSender = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), sender.getId())
                .orElseThrow();

        ChatRoomMember foundReceiver = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), receiver.getId())
                .orElseThrow();

        ChatRoomMember foundDeletedMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), deletedUser.getId())
                .orElseThrow();

        assertThat(foundSender.getUnreadCount()).isEqualTo(2);
        assertThat(foundReceiver.getUnreadCount()).isEqualTo(3);
        assertThat(foundDeletedMember.getUnreadCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("현재 사용자를 제외한 상대방의 닉네임과 탈퇴 여부를 조회한다")
    void findOpponentInfo() {
        // given
        User currentUser = createUser("08");
        User opponent = createUser("09");

        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(currentUser);
        entityManager.persist(opponent);

        createMember(chatRoom, currentUser);
        createMember(chatRoom, opponent);

        // when
        OpponentInfoProjection result = chatRoomMemberRepository
                .findOpponentInfo(chatRoom.getId(), currentUser.getId());

        // then
        assertThat(result.getNickname()).isEqualTo(opponent.getNickname());
        assertThat(result.getUserDeletedAt()).isNull();
    }

    @Test
    @DisplayName("현재 사용자를 제외한 삭제된 상대방의 정보도 조회한다")
    void findOpponentInfoWhenOpponentDeleted() {
        // given
        User currentUser = createUser("10");
        User opponent = createUser("11");

        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(currentUser);
        entityManager.persist(opponent);

        createMember(chatRoom, currentUser);
        createMember(chatRoom, opponent);

        opponent.delete();

        entityManager.flush();
        entityManager.clear();

        // when
        OpponentInfoProjection result = chatRoomMemberRepository
                .findOpponentInfo(chatRoom.getId(), currentUser.getId());

        // then
        assertThat(result.getNickname()).isEqualTo(opponent.getNickname());
        assertThat(result.getUserDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("발신자가 아닌 삭제된 채팅방 멤버를 조회한다")
    void findDeletedMembers() {
        // given
        User sender = createUser("12");
        User deletedUser = createUser("13");
        User activeUser = createUser("14");

        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(sender);
        entityManager.persist(deletedUser);
        entityManager.persist(activeUser);

        ChatRoomMember senderMember = createMember(chatRoom, sender);
        ChatRoomMember deletedMember = createMember(chatRoom, deletedUser);
        createMember(chatRoom, activeUser);

        senderMember.delete();
        deletedMember.delete();

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatRoomMember> result = chatRoomMemberRepository
                .findDeletedMembers(chatRoom.getId(), sender.getId());

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(member -> member.getUser().getId())
                .containsExactly(deletedUser.getId());
    }

    @Test
    @DisplayName("활성 멤버와 탈퇴하지 않은 사용자의 읽음 상태를 조회한다")
    void findReadStatuses() {
        // given
        User user = createUser("15");
        User deletedMemberUser = createUser("16");
        User withdrawnUser = createUser("17");

        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);
        entityManager.persist(deletedMemberUser);
        entityManager.persist(withdrawnUser);

        ChatRoomMember activeMember = createMember(chatRoom, user);
        ChatRoomMember deletedMember = createMember(chatRoom, deletedMemberUser);
        createMember(chatRoom, withdrawnUser);

        activeMember.updateLastRead(100L);
        deletedMember.delete();
        withdrawnUser.withdraw("newPassword");

        entityManager.flush();
        entityManager.clear();

        // when
        List<MemberReadStatusProjection> result =
                chatRoomMemberRepository.findReadStatuses(chatRoom.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(user.getId());
        assertThat(result.get(0).getNickname()).isEqualTo(user.getNickname());
        assertThat(result.get(0).getLastReadMessageId()).isEqualTo(100L);
        assertThat(result.get(0).getJoinedAt())
                .isEqualTo(FIXED_TIME);
    }

    @Test
    @DisplayName("삭제 여부와 관계없이 채팅방 ID와 사용자 ID로 멤버를 조회한다")
    void findByChatRoomIdAndUserId() {
        // given
        User user = createUser("18");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);

        ChatRoomMember member = createMember(chatRoom, user);
        member.delete();

        entityManager.flush();
        entityManager.clear();

        // when
        ChatRoomMember foundMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
                .orElseThrow();

        // then
        assertThat(foundMember.getId()).isEqualTo(member.getId());
        assertThat(foundMember.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("발신자 구분 없이 활성 멤버의 안 읽은 메시지 수를 증가시킨다")
    void increaseUnreadCountAll() {
        // given
        User firstUser = createUser("19");
        User secondUser = createUser("20");

        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(firstUser);
        entityManager.persist(secondUser);

        ChatRoomMember firstMember = createMember(chatRoom, firstUser);
        ChatRoomMember secondMember = createMember(chatRoom, secondUser);

        firstMember.updateUnreadCount(1);
        secondMember.updateUnreadCount(3);

        entityManager.flush();
        entityManager.clear();

        // when
        chatRoomMemberRepository.increaseUnreadCountAll(chatRoom.getId());

        entityManager.flush();
        entityManager.clear();

        // then
        ChatRoomMember foundFirstMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), firstUser.getId())
                .orElseThrow();

        ChatRoomMember foundSecondMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), secondUser.getId())
                .orElseThrow();

        assertThat(foundFirstMember.getUnreadCount()).isEqualTo(2);
        assertThat(foundSecondMember.getUnreadCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("사용자 ID로 활성 상태인 채팅방 멤버 목록을 조회한다")
    void findAllByUserId() {
        // given
        User user = createUser("21");
        User anotherUser = createUser("22");

        ChatRoom firstRoom = createGroupChatRoom("첫 번째 채팅방");
        ChatRoom secondRoom = createGroupChatRoom("두 번째 채팅방");
        ChatRoom deletedRoom = createGroupChatRoom("삭제된 멤버가 있는 채팅방");

        entityManager.persist(user);
        entityManager.persist(anotherUser);

        ChatRoomMember firstMember = createMember(firstRoom, user);
        ChatRoomMember secondMember = createMember(secondRoom, user);

        createMember(firstRoom, anotherUser);

        ChatRoomMember deletedMember = createMember(deletedRoom, user);
        deletedMember.delete();

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatRoomMember> result =
                chatRoomMemberRepository.findAllByUserId(user.getId());

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ChatRoomMember::getId)
                .containsExactlyInAnyOrder(firstMember.getId(), secondMember.getId());
    }

    @Test
    @DisplayName("채팅방 나가기 시 활성 멤버의 삭제 일시를 변경한다")
    void leave() {
        // given
        User user = createUser("23");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);

        ChatRoomMember member = createMember(chatRoom, user);
        LocalDateTime deletedAt = FIXED_DELETED_AT;

        entityManager.flush();
        entityManager.clear();

        // when
        int result = chatRoomMemberRepository.leave(member.getId(), deletedAt);

        // then
        assertThat(result).isEqualTo(1);

        ChatRoomMember foundMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
                .orElseThrow();

        assertThat(foundMember.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("이미 삭제된 멤버는 다시 나가기 처리하지 않는다")
    void leaveWhenAlreadyDeleted() {
        // given
        User user = createUser("24");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);

        ChatRoomMember member = createMember(chatRoom, user);
        member.delete();

        entityManager.flush();
        entityManager.clear();

        // when
        int result = chatRoomMemberRepository.leave(
                member.getId(),
                FIXED_DELETED_AT
        );

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("삭제된 멤버가 재입장하면 삭제 상태와 읽음 상태를 초기화한다")
    void rejoin() {
        // given
        User user = createUser("25");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);

        ChatRoomMember member = createMember(chatRoom, user);
        member.updateUnreadCount(5);
        member.updateLastRead(100L);
        member.delete();

        LocalDateTime joinedAt = FIXED_JOINED_AT;

        entityManager.flush();
        entityManager.clear();

        // when
        int result = chatRoomMemberRepository.rejoin(member.getId(), joinedAt);

        // then
        assertThat(result).isEqualTo(1);

        ChatRoomMember foundMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
                .orElseThrow();

        assertThat(foundMember.getDeletedAt()).isNull();
        assertThat(foundMember.getUnreadCount()).isZero();
        assertThat(foundMember.getLastReadMessageId()).isNull();
        assertThat(foundMember.getJoinedAt()).isEqualTo(joinedAt);
    }

    @Test
    @DisplayName("활성 멤버는 재입장 처리하지 않는다")
    void rejoinWhenMemberActive() {
        // given
        User user = createUser("26");
        ChatRoom chatRoom = createGroupChatRoom("테스트 채팅방");

        entityManager.persist(user);

        ChatRoomMember member = createMember(chatRoom, user);
        member.updateUnreadCount(5);
        member.updateLastRead(100L);

        entityManager.flush();
        entityManager.clear();

        // when
        int result = chatRoomMemberRepository.rejoin(
                member.getId(),
                FIXED_JOINED_AT
        );

        // then
        assertThat(result).isZero();

        ChatRoomMember foundMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
                .orElseThrow();

        assertThat(foundMember.getDeletedAt()).isNull();
        assertThat(foundMember.getUnreadCount()).isEqualTo(5);
        assertThat(foundMember.getLastReadMessageId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("마지막 메시지가 있는 내 채팅방 목록을 조회한다")
    void findMyChatRooms() {
        // given
        User currentUser = createUser("27");
        User opponent = createUser("28");

        ChatRoom chatRoom = ChatRoom.direct();

        entityManager.persist(currentUser);
        entityManager.persist(opponent);
        entityManager.persist(chatRoom);

        ChatRoomMember currentMember = createMember(chatRoom, currentUser);
        createMember(chatRoom, opponent);

        currentMember.updateUnreadCount(3);

        ChatMessage message = createMessage(chatRoom, opponent, "안녕하세요.");

        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatRoomListProjection> result =
                chatRoomMemberRepository.findMyChatRooms(
                        currentUser.getId(),
                        null,
                        null,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result).hasSize(1);

        ChatRoomListProjection projection = result.get(0);

        assertThat(projection.getRoomId()).isEqualTo(chatRoom.getId());
        assertThat(projection.getRoomName()).isNull();
        assertThat(projection.getOpponentNickname()).isEqualTo(opponent.getNickname());
        assertThat(projection.getLastMessage()).isEqualTo(message.getContent());
        assertThat(projection.getLastMessageAt()).isNotNull();
        assertThat(projection.getUnreadCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("마지막 메시지가 없는 채팅방도 목록에 포함한다")
    void findMyChatRoomsWhenLastMessageIsNull() {
        // given
        User user = createUser("29");
        ChatRoom chatRoom = createGroupChatRoom("메시지가 없는 채팅방");

        entityManager.persist(user);
        createMember(chatRoom, user);

        // when
        List<ChatRoomListProjection> result =
                chatRoomMemberRepository.findMyChatRooms(
                        user.getId(),
                        null,
                        null,
                        PageRequest.of(0, 10)
                );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomId()).isEqualTo(chatRoom.getId());
        assertThat(result.get(0).getLastMessage()).isNull();
        assertThat(result.get(0).getLastMessageAt()).isNull();
    }

    @Test
    @DisplayName("메시지가 없는 채팅방도 null 시간과 채팅방 ID를 기준으로 다음 페이지를 조회한다")
    void findMyChatRoomsWithCursorWhenLastMessageIsNull() {
        // given
        User user = createUser("30");
        User opponent = createUser("31");

        entityManager.persist(user);
        entityManager.persist(opponent);

        ChatRoom messageRoom = ChatRoom.direct();
        ChatRoom firstNullRoom = ChatRoom.direct();
        ChatRoom secondNullRoom = ChatRoom.direct();

        entityManager.persist(messageRoom);
        entityManager.persist(firstNullRoom);
        entityManager.persist(secondNullRoom);

        createMember(messageRoom, user);
        createMember(messageRoom, opponent);

        createMember(firstNullRoom, user);
        createMember(firstNullRoom, opponent);

        createMember(secondNullRoom, user);
        createMember(secondNullRoom, opponent);

        // 메시지가 있는 채팅방만 마지막 메시지를 설정한다.
        createMessage(messageRoom, opponent, "마지막 메시지");

        entityManager.flush();
        entityManager.clear();

        // when
        // 첫 번째 페이지: 메시지가 있는 채팅방 조회
        List<ChatRoomListProjection> firstPage =
                chatRoomMemberRepository.findMyChatRooms(
                        user.getId(),
                        null,
                        null,
                        PageRequest.of(0, 1)
                );

        ChatRoomListProjection firstCursor = firstPage.get(0);

        // 두 번째 페이지: 메시지가 없는 채팅방 조회
        // 이전 커서의 메시지 시간이 존재하지만,
        // 다음 페이지에는 마지막 메시지가 없는 채팅방도 포함되어야 한다.
        List<ChatRoomListProjection> secondPage =
                chatRoomMemberRepository.findMyChatRooms(
                        user.getId(),
                        firstCursor.getLastMessageAt(),
                        firstCursor.getRoomId(),
                        PageRequest.of(0, 1)
                );

        ChatRoomListProjection secondCursor = secondPage.get(0);

        // 세 번째 페이지: null 시간과 채팅방 ID를 기준으로 조회
        // 이전 페이지의 마지막 메시지 시간이 null이므로
        // roomId가 더 작은 메시지 없는 채팅방을 조회한다.
        List<ChatRoomListProjection> thirdPage =
                chatRoomMemberRepository.findMyChatRooms(
                        user.getId(),
                        secondCursor.getLastMessageAt(),
                        secondCursor.getRoomId(),
                        PageRequest.of(0, 1)
                );

        // 네 번째 페이지: 더 이상 조회할 채팅방이 없는지 확인
        List<ChatRoomListProjection> lastPage =
                chatRoomMemberRepository.findMyChatRooms(
                        user.getId(),
                        thirdPage.get(0).getLastMessageAt(),
                        thirdPage.get(0).getRoomId(),
                        PageRequest.of(0, 1)
                );

        // then
        assertThat(firstPage).hasSize(1);
        assertThat(firstCursor.getRoomId()).isEqualTo(messageRoom.getId());
        assertThat(firstCursor.getLastMessageAt()).isNotNull();

        assertThat(secondPage).hasSize(1);
        assertThat(secondCursor.getRoomId()).isEqualTo(secondNullRoom.getId());
        assertThat(secondCursor.getLastMessageAt()).isNull();

        assertThat(thirdPage).hasSize(1);
        assertThat(thirdPage.get(0).getRoomId()).isEqualTo(firstNullRoom.getId());
        assertThat(thirdPage.get(0).getLastMessageAt()).isNull();

        assertThat(lastPage).isEmpty();

        assertThat(List.of(
                firstCursor.getRoomId(),
                secondCursor.getRoomId(),
                thirdPage.get(0).getRoomId()
        )).containsExactly(
                messageRoom.getId(),
                secondNullRoom.getId(),
                firstNullRoom.getId()
        );
    }
}