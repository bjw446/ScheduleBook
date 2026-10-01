package com.example.schedulebook.domain.chatmessage.publisher;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.common.websocket.publisher.WebSocketPublisher;
import com.example.schedulebook.domain.chatmessage.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.event.ChatMessageDeletedEvent;
import com.example.schedulebook.domain.chatmessage.event.ReadMessageEvent;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessagePublisherTest {

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    @Mock
    private WebSocketPublisher webSocketPublisher;

    @Mock
    private ChatMessage chatMessage;

    @Mock
    private ChatMessage secondChatMessage;

    @Mock
    private ChatRoom chatRoom;

    @Mock
    private User sender;

    private ChatMessagePublisher chatMessagePublisher;

    private static final Long ROOM_ID = 1L;
    private static final Long MESSAGE_ID = 10L;
    private static final Long SECOND_MESSAGE_ID = 20L;
    private static final Long SENDER_ID = 100L;
    private static final Long CURRENT_USER_ID = 200L;
    private static final Long LAST_READ_MESSAGE_ID = 5L;

    private static final String SENDER_NICKNAME = "테스트 사용자";
    private static final String CONTENT = "안녕하세요.";
    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 10, 1, 15, 0);

    @BeforeEach
    void setUp() {
        chatMessagePublisher =
                new ChatMessagePublisher(
                        afterCommitExecutor,
                        webSocketPublisher
                );
    }

    @Test
    void 메시지를_채팅방_destination으로_전송한다() {
        // given
        int unreadCount = 3;

        givenChatMessage(
                chatMessage,
                MESSAGE_ID,
                ROOM_ID,
                SENDER_ID,
                SENDER_NICKNAME
        );

        // when
        chatMessagePublisher.publishMessage(
                chatMessage,
                unreadCount
        );

        // then
        ArgumentCaptor<ChatMessageResponse> responseCaptor =
                ArgumentCaptor.forClass(ChatMessageResponse.class);

        verify(webSocketPublisher).sendAfterCommit(
                eq(WebSocketDestination.getChatDestination(ROOM_ID)),
                responseCaptor.capture()
        );

        ChatMessageResponse response =
                responseCaptor.getValue();

        assertThat(response.messageId())
                .isEqualTo(MESSAGE_ID);
        assertThat(response.roomId())
                .isEqualTo(ROOM_ID);
        assertThat(response.senderId())
                .isEqualTo(SENDER_ID);
        assertThat(response.senderNickname())
                .isEqualTo(SENDER_NICKNAME);
        assertThat(response.content())
                .isEqualTo(CONTENT);
        assertThat(response.unreadMemberCount())
                .isEqualTo(unreadCount);
        assertThat(response.createdAt())
                .isEqualTo(CREATED_AT);
    }

    @Test
    void 발신자가_없는_메시지도_채팅방_destination으로_전송한다() {
        // given
        int unreadCount = 2;

        given(chatMessage.getId())
                .willReturn(MESSAGE_ID);
        given(chatMessage.getChatRoom())
                .willReturn(chatRoom);
        given(chatRoom.getId())
                .willReturn(ROOM_ID);
        given(chatMessage.getSender())
                .willReturn(null);
        given(chatMessage.getContent())
                .willReturn(CONTENT);
        given(chatMessage.getReplyMessage())
                .willReturn(null);
        given(chatMessage.isEdited())
                .willReturn(false);
        given(chatMessage.getCreatedAt())
                .willReturn(CREATED_AT);

        // when
        chatMessagePublisher.publishMessage(
                chatMessage,
                unreadCount
        );

        // then
        ArgumentCaptor<ChatMessageResponse> responseCaptor =
                ArgumentCaptor.forClass(ChatMessageResponse.class);

        verify(webSocketPublisher).sendAfterCommit(
                eq(WebSocketDestination.getChatDestination(ROOM_ID)),
                responseCaptor.capture()
        );

        ChatMessageResponse response =
                responseCaptor.getValue();

        assertThat(response.messageId())
                .isEqualTo(MESSAGE_ID);
        assertThat(response.roomId())
                .isEqualTo(ROOM_ID);
        assertThat(response.senderId())
                .isNull();
        assertThat(response.senderNickname())
                .isNull();
        assertThat(response.unreadMemberCount())
                .isEqualTo(unreadCount);
    }

    @Test
    void 읽음_메시지를_채팅방_read_destination으로_전송한다() {
        // when
        chatMessagePublisher.publishReadMessageAfterCommit(
                ROOM_ID,
                CURRENT_USER_ID,
                LAST_READ_MESSAGE_ID
        );

        // then
        ArgumentCaptor<ReadMessageEvent> eventCaptor =
                ArgumentCaptor.forClass(ReadMessageEvent.class);

        verify(webSocketPublisher).sendAfterCommit(
                eq(WebSocketDestination.getChatReadDestination(ROOM_ID)),
                eventCaptor.capture()
        );

        ReadMessageEvent event = eventCaptor.getValue();

        assertThat(event.roomId())
                .isEqualTo(ROOM_ID);
        assertThat(event.userId())
                .isEqualTo(CURRENT_USER_ID);
        assertThat(event.lastReadMessageId())
                .isEqualTo(LAST_READ_MESSAGE_ID);
        assertThat(event.readAt())
                .isNotNull();
    }

    @Test
    void 삭제된_메시지를_채팅방_delete_destination으로_전송한다() {
        // when
        chatMessagePublisher.publishDeleteMessageAfterCommit(
                ROOM_ID,
                MESSAGE_ID
        );

        // then
        ArgumentCaptor<ChatMessageDeletedEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatMessageDeletedEvent.class);

        verify(webSocketPublisher).sendAfterCommit(
                eq(WebSocketDestination.getChatDeleteDestination(ROOM_ID)),
                eventCaptor.capture()
        );

        ChatMessageDeletedEvent event =
                eventCaptor.getValue();

        assertThat(event.roomId())
                .isEqualTo(ROOM_ID);
        assertThat(event.messageId())
                .isEqualTo(MESSAGE_ID);
        assertThat(event.content())
                .isEqualTo(CommonConst.DELETED_MESSAGE);
    }

    @Test
    void 여러_메시지를_afterCommit_이후_각각_전송한다() {
        // given
        int firstUnreadCount = 3;
        int secondUnreadCount = 5;

        givenChatMessage(
                chatMessage,
                MESSAGE_ID,
                ROOM_ID,
                SENDER_ID,
                SENDER_NICKNAME
        );

        givenChatMessage(
                secondChatMessage,
                SECOND_MESSAGE_ID,
                ROOM_ID,
                SENDER_ID,
                SENDER_NICKNAME
        );

        PublishChatMessage firstMessage =
                new PublishChatMessage(
                        chatMessage,
                        firstUnreadCount
                );

        PublishChatMessage secondMessage =
                new PublishChatMessage(
                        secondChatMessage,
                        secondUnreadCount
                );

        // afterCommitExecutor에 등록된 Runnable을 실제 실행한다.
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(afterCommitExecutor)
                .execute(any(Runnable.class));

        // when
        chatMessagePublisher.publishMessages(
                List.of(firstMessage, secondMessage)
        );

        // then
        verify(afterCommitExecutor)
                .execute(any(Runnable.class));

        ArgumentCaptor<String> destinationCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<ChatMessageResponse> responseCaptor =
                ArgumentCaptor.forClass(ChatMessageResponse.class);

        verify(webSocketPublisher, times(2)).send(
                destinationCaptor.capture(),
                responseCaptor.capture()
        );

        assertThat(destinationCaptor.getAllValues())
                .containsExactly(
                        WebSocketDestination.getChatDestination(ROOM_ID),
                        WebSocketDestination.getChatDestination(ROOM_ID)
                );

        List<ChatMessageResponse> responses =
                responseCaptor.getAllValues();

        assertThat(responses)
                .extracting(ChatMessageResponse::messageId)
                .containsExactly(
                        MESSAGE_ID,
                        SECOND_MESSAGE_ID
                );

        assertThat(responses)
                .extracting(ChatMessageResponse::unreadMemberCount)
                .containsExactly(
                        firstUnreadCount,
                        secondUnreadCount
                );
    }

    @Test
    void 빈_메시지_목록은_afterCommit만_등록하고_메시지를_전송하지_않는다() {
        // given
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(afterCommitExecutor)
                .execute(any(Runnable.class));

        // when
        chatMessagePublisher.publishMessages(List.of());

        // then
        verify(afterCommitExecutor)
                .execute(any(Runnable.class));

        verifyNoInteractions(webSocketPublisher);
    }

    private void givenChatMessage(
            ChatMessage target,
            Long messageId,
            Long roomId,
            Long senderId,
            String senderNickname
    ) {
        given(target.getId())
                .willReturn(messageId);

        given(target.getChatRoom())
                .willReturn(chatRoom);

        given(chatRoom.getId())
                .willReturn(roomId);

        given(target.getSender())
                .willReturn(sender);

        given(sender.getId())
                .willReturn(senderId);

        given(sender.getNickname())
                .willReturn(senderNickname);

        given(target.getContent())
                .willReturn(CONTENT);

        given(target.getReplyMessage())
                .willReturn(null);

        given(target.isEdited())
                .willReturn(false);

        given(target.getCreatedAt())
                .willReturn(CREATED_AT);
    }
}