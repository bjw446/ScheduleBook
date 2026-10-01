package com.example.schedulebook.domain.chatmessage.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.chatmessage.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatmessage.enums.SystemMessageType;
import com.example.schedulebook.domain.chatmessage.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageManagerTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatUnreadCountManager chatUnreadCountManager;

    @Mock
    private ChatRoom chatRoom;

    @Mock
    private User owner;

    @Mock
    private User inviter;

    @Mock
    private MemberReadStatusProjection readStatus;

    private ChatMessageManager chatMessageManager;

    private static final Long ROOM_ID = 1L;
    private static final int UNREAD_COUNT = 3;

    @BeforeEach
    void setUp() {
        chatMessageManager = new ChatMessageManager(
                chatMessageRepository,
                chatRoomMemberRepository,
                chatUnreadCountManager
        );

        when(chatRoom.getId()).thenReturn(ROOM_ID);
    }

    @Test
    void createLeaveSystemMessage_퇴장_시스템_메시지를_생성한다() {
        // given
        String nickname = "홍길동";
        whenReadStatusAndUnreadCount();

        // when
        PublishChatMessage result =
                chatMessageManager.createLeaveSystemMessage(chatRoom, nickname);

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        assertThat(savedMessage.getChatRoom()).isSameAs(chatRoom);
        assertThat(savedMessage.getChatMessageType()).isEqualTo(ChatMessageType.SYSTEM);
        assertThat(savedMessage.getSystemMessageType()).isEqualTo(SystemMessageType.USER_LEAVE);
        assertThat(savedMessage.getContent())
                .isEqualTo(SystemMessageType.USER_LEAVE.format(nickname));

        assertThat(result.chatMessage()).isSameAs(savedMessage);
        assertThat(result.unreadCount()).isEqualTo(UNREAD_COUNT);

        verifyCommonPublishFlow(savedMessage);
    }

    @Test
    void createGroupRoomSystemMessage_그룹방_생성_시스템_메시지를_생성한다() {
        // given
        String nickname = "방장";
        when(owner.getNickname()).thenReturn(nickname);
        whenReadStatusAndUnreadCount();

        // when
        PublishChatMessage result =
                chatMessageManager.createGroupRoomSystemMessage(chatRoom, owner);

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        assertThat(savedMessage.getChatRoom()).isSameAs(chatRoom);
        assertThat(savedMessage.getChatMessageType()).isEqualTo(ChatMessageType.SYSTEM);
        assertThat(savedMessage.getSystemMessageType())
                .isEqualTo(SystemMessageType.GROUP_ROOM_CREATED);
        assertThat(savedMessage.getContent())
                .isEqualTo(SystemMessageType.GROUP_ROOM_CREATED.format(nickname));

        assertThat(result.chatMessage()).isSameAs(savedMessage);
        assertThat(result.unreadCount()).isEqualTo(UNREAD_COUNT);

        verifyCommonPublishFlow(savedMessage);
    }

    @Test
    void createInviteSystemMessage_초대된_사용자_이름을_정상적으로_생성한다() {
        // given
        String inviterNickname = "초대자";

        User user1 = mockUser("철수");
        User user2 = mockUser("영희");

        when(inviter.getNickname()).thenReturn(inviterNickname);
        whenReadStatusAndUnreadCount();

        // when
        PublishChatMessage result =
                chatMessageManager.createInviteSystemMessage(
                        chatRoom,
                        inviter,
                        List.of(user1, user2)
                );

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        assertThat(savedMessage.getSystemMessageType())
                .isEqualTo(SystemMessageType.USER_INVITED);

        assertThat(savedMessage.getContent())
                .isEqualTo("초대자님이 철수님, 영희님을 초대했습니다.");

        assertThat(result.chatMessage()).isSameAs(savedMessage);
        assertThat(result.unreadCount()).isEqualTo(UNREAD_COUNT);

        verifyCommonPublishFlow(savedMessage);
    }

    @Test
    void createInviteSystemMessage_최대_이름_수_이내라면_모든_이름을_표시한다() {
        // given
        List<User> users = createUsers(CommonConst.MAX_NAMES);

        when(inviter.getNickname()).thenReturn("초대자");
        whenReadStatusAndUnreadCount();

        // when
        chatMessageManager.createInviteSystemMessage(
                chatRoom,
                inviter,
                users
        );

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        String expectedNames = users.stream()
                .map(user -> user.getNickname() + "님")
                .reduce((first, second) -> first + ", " + second)
                .orElse("");

        assertThat(savedMessage.getContent())
                .isEqualTo("초대자님이 " + expectedNames + "을 초대했습니다.");
    }

    @Test
    void createInviteSystemMessage_최대_이름_수를_초과하면_외_몇명_형식으로_표시한다() {
        // given
        int userCount = CommonConst.MAX_NAMES + 1;
        List<User> users = createUsers(userCount);

        when(inviter.getNickname()).thenReturn("초대자");
        whenReadStatusAndUnreadCount();

        // when
        chatMessageManager.createInviteSystemMessage(
                chatRoom,
                inviter,
                users
        );

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        String firstNames = java.util.stream.IntStream
                .rangeClosed(1, CommonConst.MAX_NAMES)
                .mapToObj(i -> "사용자" + i + "님")
                .reduce((first, second) -> first + ", " + second)
                .orElse("");

        String expectedContent =
                "초대자님이 " +
                        firstNames +
                        " 외 1명을 초대했습니다.";

        assertThat(savedMessage.getContent())
                .isEqualTo(expectedContent);
    }

    @Test
    void createUpdateNameSystemMessage_방_이름_변경_시스템_메시지를_생성한다() {
        // given
        when(inviter.getNickname()).thenReturn("변경자");
        whenReadStatusAndUnreadCount();

        // when
        PublishChatMessage result =
                chatMessageManager.createUpdateNameSystemMessage(
                        chatRoom,
                        inviter,
                        "기존방",
                        "새로운방"
                );

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        assertThat(savedMessage.getSystemMessageType())
                .isEqualTo(SystemMessageType.ROOM_NAME_UPDATED);

        assertThat(savedMessage.getContent())
                .isEqualTo("변경자님이 채팅방 이름을 변경했습니다. (기존방 -> 새로운방)");

        assertThat(result.chatMessage()).isSameAs(savedMessage);
        assertThat(result.unreadCount()).isEqualTo(UNREAD_COUNT);

        verifyCommonPublishFlow(savedMessage);
    }

    @Test
    void createScheduleUpdatedSystemMessage_일정_수정_시스템_메시지를_생성한다() {
        // given
        whenReadStatusAndUnreadCount();

        // when
        PublishChatMessage result =
                chatMessageManager.createScheduleUpdatedSystemMessage(chatRoom);

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        assertThat(savedMessage.getSystemMessageType())
                .isEqualTo(SystemMessageType.SCHEDULE_UPDATED);

        assertThat(savedMessage.getContent())
                .isEqualTo(SystemMessageType.SCHEDULE_UPDATED.getMessage());

        assertThat(result.chatMessage()).isSameAs(savedMessage);
        assertThat(result.unreadCount()).isEqualTo(UNREAD_COUNT);

        verifyCommonPublishFlow(savedMessage);
    }

    @Test
    void createScheduleShareCanceledSystemMessage_일정_공유_취소_시스템_메시지를_생성한다() {
        // given
        whenReadStatusAndUnreadCount();

        // when
        PublishChatMessage result =
                chatMessageManager.createScheduleShareCanceledSystemMessage(chatRoom);

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        assertThat(savedMessage.getSystemMessageType())
                .isEqualTo(SystemMessageType.SCHEDULE_SHARE_CANCELED);

        assertThat(savedMessage.getContent())
                .isEqualTo(SystemMessageType.SCHEDULE_SHARE_CANCELED.getMessage());

        assertThat(result.chatMessage()).isSameAs(savedMessage);
        assertThat(result.unreadCount()).isEqualTo(UNREAD_COUNT);

        verifyCommonPublishFlow(savedMessage);
    }

    @Test
    void createScheduleDeletedSystemMessage_일정_삭제_시스템_메시지를_생성한다() {
        // given
        whenReadStatusAndUnreadCount();

        // when
        PublishChatMessage result =
                chatMessageManager.createScheduleDeletedSystemMessage(chatRoom);

        // then
        ChatMessage savedMessage = verifyAndCaptureSavedMessage();

        assertThat(savedMessage.getSystemMessageType())
                .isEqualTo(SystemMessageType.SCHEDULE_DELETED);

        assertThat(savedMessage.getContent())
                .isEqualTo(SystemMessageType.SCHEDULE_DELETED.getMessage());

        assertThat(result.chatMessage()).isSameAs(savedMessage);
        assertThat(result.unreadCount()).isEqualTo(UNREAD_COUNT);

        verifyCommonPublishFlow(savedMessage);
    }

    private void whenReadStatusAndUnreadCount() {
        List<MemberReadStatusProjection> readStatuses = List.of(readStatus);

        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(readStatuses);

        when(chatUnreadCountManager.calculateUnreadCount(
                any(ChatMessage.class),
                eq(readStatuses)
        )).thenReturn(UNREAD_COUNT);
    }

    private ChatMessage verifyAndCaptureSavedMessage() {
        ArgumentCaptor<ChatMessage> captor =
                ArgumentCaptor.forClass(ChatMessage.class);

        verify(chatMessageRepository).save(captor.capture());

        return captor.getValue();
    }

    private void verifyCommonPublishFlow(ChatMessage savedMessage) {
        verify(chatRoom).updateLastMessage(savedMessage);
        verify(chatRoomMemberRepository).increaseUnreadCountAll(ROOM_ID);
        verify(chatRoomMemberRepository).findReadStatuses(ROOM_ID);
        verify(chatUnreadCountManager).calculateUnreadCount(
                savedMessage,
                List.of(readStatus)
        );
    }

    private User mockUser(String nickname) {
        User user = mock(User.class);
        when(user.getNickname()).thenReturn(nickname);
        return user;
    }

    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            User user = mock(User.class);

            if (i <= CommonConst.MAX_NAMES) {
                when(user.getNickname()).thenReturn("사용자" + i);
            }

            users.add(user);
        }

        return users;
    }
}