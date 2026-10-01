package com.example.schedulebook.domain.chatmessage.service;

import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatUnreadCountManagerTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatMessage chatMessage;

    @Mock
    private User sender;

    @Mock
    private ChatRoomMember chatRoomMember;

    @Mock
    private ChatRoom chatRoom;

    @Mock
    private User user;

    private ChatUnreadCountManager chatUnreadCountManager;

    private static final Long MESSAGE_ID = 100L;
    private static final Long SENDER_ID = 1L;
    private static final Long EXCLUDED_USER_ID = 1L;
    private static final Long CHAT_ROOM_ID = 10L;

    private static final LocalDateTime MESSAGE_CREATED_AT =
            LocalDateTime.of(2026, 1, 10, 10, 0);

    private static final LocalDateTime JOINED_AT =
            LocalDateTime.of(2026, 1, 1, 10, 0);

    @BeforeEach
    void setUp() {
        chatUnreadCountManager =
                new ChatUnreadCountManager(chatMessageRepository);
    }

    @Test
    void calculateUnreadCount_읽지않은_사용자_수를_계산한다() {
        // given
        MemberReadStatusProjection unreadMember1 =
                createMember(2L);
        // getJoinedAt()이 null이면 isBefore 비교 시 NPE가 발생하므로 JOINED_AT 설정 추가
        when(unreadMember1.getJoinedAt()).thenReturn(JOINED_AT);

        MemberReadStatusProjection unreadMember2 =
                createMember(3L);
        when(unreadMember2.getLastReadMessageId()).thenReturn(90L);
        when(unreadMember2.getJoinedAt()).thenReturn(JOINED_AT);

        // 현재 메시지를 이미 읽었으므로 joinedAt까지 내려가지 않는다.
        MemberReadStatusProjection readMember =
                createMember(4L);
        when(readMember.getLastReadMessageId()).thenReturn(MESSAGE_ID);

        // 발신자는 첫 번째 filter에서 제외된다.
        MemberReadStatusProjection senderMember =
                createMember(SENDER_ID);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getSender()).thenReturn(sender);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);
        when(sender.getId()).thenReturn(SENDER_ID);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        List.of(
                                unreadMember1,
                                unreadMember2,
                                readMember,
                                senderMember
                        )
                );

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    void calculateUnreadCount_발신자가_null이면_발신자_제외없이_계산한다() {
        // given
        MemberReadStatusProjection unreadMember1 =
                createMember(2L);
        when(unreadMember1.getJoinedAt()).thenReturn(JOINED_AT);

        MemberReadStatusProjection unreadMember2 =
                createMember(3L);
        when(unreadMember2.getLastReadMessageId()).thenReturn(90L);
        when(unreadMember2.getJoinedAt()).thenReturn(JOINED_AT);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getSender()).thenReturn(null);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        List.of(unreadMember1, unreadMember2)
                );

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    void calculateUnreadCount_마지막_읽은_메시지가_현재_메시지보다_작으면_unread로_계산한다() {
        // given
        MemberReadStatusProjection member =
                createMember(2L);

        when(member.getLastReadMessageId())
                .thenReturn(MESSAGE_ID - 1);

        when(member.getJoinedAt())
                .thenReturn(JOINED_AT);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        List.of(member)
                );

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    void calculateUnreadCount_마지막_읽은_메시지가_현재_메시지와_같으면_읽은_것으로_계산한다() {
        // given
        MemberReadStatusProjection member =
                createMember(2L);

        when(member.getLastReadMessageId())
                .thenReturn(MESSAGE_ID);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        List.of(member)
                );

        // then
        assertThat(result).isZero();
    }

    @Test
    void calculateUnreadCount_마지막_읽은_메시지가_null이면_unread로_계산한다() {
        // given
        MemberReadStatusProjection member =
                createMember(2L);

        when(member.getJoinedAt()).thenReturn(JOINED_AT);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        List.of(member)
                );

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    void calculateUnreadCount_메시지가_입장_이후에_생성되면_unread로_계산한다() {
        // given
        MemberReadStatusProjection member =
                createMember(2L);

        when(member.getJoinedAt()).thenReturn(JOINED_AT);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        List.of(member)
                );

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    void calculateUnreadCount_메시지가_입장_이전에_생성되면_unread에서_제외한다() {
        // given
        LocalDateTime joinedAt =
                MESSAGE_CREATED_AT.plusHours(1);

        MemberReadStatusProjection member =
                createMember(2L);

        when(member.getJoinedAt()).thenReturn(joinedAt);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        List.of(member)
                );

        // then
        assertThat(result).isZero();
    }

    @Test
    void calculateUnreadCount_입장_시각과_메시지_생성_시각이_같으면_unread로_계산한다() {
        // given
        MemberReadStatusProjection member =
                createMember(2L);

        when(member.getJoinedAt()).thenReturn(MESSAGE_CREATED_AT);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        List.of(member)
                );

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    void calculateUnreadCount_특정_사용자를_제외하고_unread_수를_계산한다() {
        // given
        MemberReadStatusProjection excludedMember =
                createMember(EXCLUDED_USER_ID);

        MemberReadStatusProjection unreadMember =
                createMember(2L);

        when(unreadMember.getJoinedAt()).thenReturn(JOINED_AT);

        MemberReadStatusProjection readMember =
                createMember(3L);

        when(readMember.getLastReadMessageId())
                .thenReturn(MESSAGE_ID);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        EXCLUDED_USER_ID,
                        List.of(
                                excludedMember,
                                unreadMember,
                                readMember
                        )
                );

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    void calculateUnreadCount_특정_사용자_제외_후_입장_이전_메시지는_unread에서_제외한다() {
        // given
        MemberReadStatusProjection member =
                createMember(2L);

        when(member.getJoinedAt())
                .thenReturn(MESSAGE_CREATED_AT.plusMinutes(1));

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(chatMessage.getCreatedAt()).thenReturn(MESSAGE_CREATED_AT);

        // when
        int result =
                chatUnreadCountManager.calculateUnreadCount(
                        chatMessage,
                        EXCLUDED_USER_ID,
                        List.of(member)
                );

        // then
        assertThat(result).isZero();
    }

    @Test
    void recalculateUnreadCount_마지막_읽은_메시지가_있으면_해당_ID를_전달한다() {
        // given
        Long lastReadMessageId = 90L;

        when(chatRoomMember.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getId()).thenReturn(CHAT_ROOM_ID);

        when(chatRoomMember.getLastReadMessageId())
                .thenReturn(lastReadMessageId);

        when(chatRoomMember.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(SENDER_ID);

        when(chatRoomMember.getJoinedAt()).thenReturn(JOINED_AT);

        when(chatMessageRepository.countUnreadMessages(
                CHAT_ROOM_ID,
                lastReadMessageId,
                SENDER_ID,
                JOINED_AT
        )).thenReturn(5L);

        // when
        long result =
                chatUnreadCountManager.recalculateUnreadCount(
                        chatRoomMember
                );

        // then
        assertThat(result).isEqualTo(5L);

        verify(chatMessageRepository).countUnreadMessages(
                CHAT_ROOM_ID,
                lastReadMessageId,
                SENDER_ID,
                JOINED_AT
        );
    }

    @Test
    void recalculateUnreadCount_마지막_읽은_메시지가_null이면_0을_전달한다() {
        // given
        when(chatRoomMember.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getId()).thenReturn(CHAT_ROOM_ID);

        when(chatRoomMember.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(SENDER_ID);

        when(chatRoomMember.getJoinedAt()).thenReturn(JOINED_AT);

        when(chatMessageRepository.countUnreadMessages(
                CHAT_ROOM_ID,
                0L,
                SENDER_ID,
                JOINED_AT
        )).thenReturn(7L);

        // when
        long result =
                chatUnreadCountManager.recalculateUnreadCount(
                        chatRoomMember
                );

        // then
        assertThat(result).isEqualTo(7L);

        verify(chatMessageRepository).countUnreadMessages(
                CHAT_ROOM_ID,
                0L,
                SENDER_ID,
                JOINED_AT
        );
    }

    private MemberReadStatusProjection createMember(Long userId) {
        MemberReadStatusProjection member =
                mock(MemberReadStatusProjection.class);

        lenient().when(member.getUserId()).thenReturn(userId);

        return member;
    }
}