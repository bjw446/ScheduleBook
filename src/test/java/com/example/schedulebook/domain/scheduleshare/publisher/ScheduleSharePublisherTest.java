package com.example.schedulebook.domain.scheduleshare.publisher;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.common.websocket.publisher.WebSocketPublisher;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.SchedulePreviewResponse;
import com.example.schedulebook.domain.schedulesnapshot.enums.SchedulePreviewState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleSharePublisherTest {

    private static final Long ROOM_ID = 1L;
    private static final String CHAT_DESTINATION = "/sub/chat/rooms/" + ROOM_ID;

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    @Mock
    private WebSocketPublisher webSocketPublisher;

    @Mock
    private ChatMessage chatMessage;

    @Mock
    private ChatMessageResponse chatMessageResponse;

    @Mock
    private SchedulePreviewResponse schedulePreviewResponse;

    private ScheduleSharePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ScheduleSharePublisher(
                afterCommitExecutor,
                webSocketPublisher
        );
    }

    @Test
    void 일정_공유_메시지를_정상_발행한다() {
        // given
        int unreadCount = 3;

        when(chatMessageResponse.roomId())
                .thenReturn(ROOM_ID);

        try (
                MockedStatic<SchedulePreviewResponse> previewMock =
                        mockStatic(SchedulePreviewResponse.class);
                MockedStatic<ChatMessageResponse> responseMock =
                        mockStatic(ChatMessageResponse.class);
                MockedStatic<WebSocketDestination> destinationMock =
                        mockStatic(WebSocketDestination.class)
        ) {
            previewMock.when(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.NORMAL)
                    )
            ).thenReturn(schedulePreviewResponse);

            responseMock.when(() ->
                    ChatMessageResponse.from(
                            eq(chatMessage),
                            eq(unreadCount),
                            eq(schedulePreviewResponse)
                    )
            ).thenReturn(chatMessageResponse);

            destinationMock.when(() ->
                    WebSocketDestination.getChatDestination(ROOM_ID)
            ).thenReturn(CHAT_DESTINATION);

            // when
            publisher.publishScheduleShared(chatMessage, unreadCount);

            // then
            previewMock.verify(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.NORMAL)
                    )
            );

            responseMock.verify(() ->
                    ChatMessageResponse.from(
                            chatMessage,
                            unreadCount,
                            schedulePreviewResponse
                    )
            );

            destinationMock.verify(() ->
                    WebSocketDestination.getChatDestination(ROOM_ID)
            );

            verify(webSocketPublisher)
                    .sendAfterCommit(
                            CHAT_DESTINATION,
                            chatMessageResponse
                    );

            verifyNoInteractions(afterCommitExecutor);
        }
    }

    @Test
    void 일정이_수정되면_커밋_이후_수정된_상태로_메시지를_발행한다() {
        // given
        ChatMessage firstMessage = mock(ChatMessage.class);
        ChatMessage secondMessage = mock(ChatMessage.class);

        ChatMessageResponse firstResponse = mock(ChatMessageResponse.class);
        ChatMessageResponse secondResponse = mock(ChatMessageResponse.class);

        SchedulePreviewResponse firstPreview =
                mock(SchedulePreviewResponse.class);
        SchedulePreviewResponse secondPreview =
                mock(SchedulePreviewResponse.class);

        when(firstResponse.roomId()).thenReturn(ROOM_ID);
        when(secondResponse.roomId()).thenReturn(2L);

        try (
                MockedStatic<SchedulePreviewResponse> previewMock =
                        mockStatic(SchedulePreviewResponse.class);
                MockedStatic<ChatMessageResponse> responseMock =
                        mockStatic(ChatMessageResponse.class);
                MockedStatic<WebSocketDestination> destinationMock =
                        mockStatic(WebSocketDestination.class)
        ) {
            previewMock.when(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.EDITED)
                    )
            ).thenReturn(firstPreview, secondPreview);

            responseMock.when(() ->
                    ChatMessageResponse.from(
                            firstMessage,
                            0,
                            firstPreview
                    )
            ).thenReturn(firstResponse);

            responseMock.when(() ->
                    ChatMessageResponse.from(
                            secondMessage,
                            0,
                            secondPreview
                    )
            ).thenReturn(secondResponse);

            destinationMock.when(() ->
                    WebSocketDestination.getChatDestination(ROOM_ID)
            ).thenReturn(CHAT_DESTINATION);

            destinationMock.when(() ->
                    WebSocketDestination.getChatDestination(2L)
            ).thenReturn("/sub/chat/rooms/2");

            // Runnable을 실행하지 않고 캡처한다.
            ArgumentCaptor<Runnable> runnableCaptor =
                    ArgumentCaptor.forClass(Runnable.class);

            // when
            publisher.publishScheduleUpdated(
                    List.of(firstMessage, secondMessage)
            );

            // then - 커밋 콜백 등록까지만 수행되었는지 검증
            verify(afterCommitExecutor)
                    .execute(runnableCaptor.capture());

            verifyNoInteractions(webSocketPublisher);

            // when - 실제 커밋 이후 콜백 실행
            runnableCaptor.getValue().run();

            // then - 콜백 실행 이후 WebSocket 발행
            verify(webSocketPublisher)
                    .send(CHAT_DESTINATION, firstResponse);

            verify(webSocketPublisher)
                    .send("/sub/chat/rooms/2", secondResponse);

            verify(webSocketPublisher, never())
                    .sendAfterCommit(anyString(), any());
        }
    }

    @Test
    void 일정_수락_메시지를_수락된_상태로_발행한다() {
        // given
        when(chatMessageResponse.roomId())
                .thenReturn(ROOM_ID);

        try (
                MockedStatic<SchedulePreviewResponse> previewMock =
                        mockStatic(SchedulePreviewResponse.class);
                MockedStatic<ChatMessageResponse> responseMock =
                        mockStatic(ChatMessageResponse.class);
                MockedStatic<WebSocketDestination> destinationMock =
                        mockStatic(WebSocketDestination.class)
        ) {
            previewMock.when(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.ACCEPTED)
                    )
            ).thenReturn(schedulePreviewResponse);

            responseMock.when(() ->
                    ChatMessageResponse.from(
                            chatMessage,
                            0,
                            schedulePreviewResponse
                    )
            ).thenReturn(chatMessageResponse);

            destinationMock.when(() ->
                    WebSocketDestination.getChatDestination(ROOM_ID)
            ).thenReturn(CHAT_DESTINATION);

            // when
            publisher.publishAcceptSharedSchedule(chatMessage);

            // then
            previewMock.verify(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.ACCEPTED)
                    )
            );

            responseMock.verify(() ->
                    ChatMessageResponse.from(
                            chatMessage,
                            0,
                            schedulePreviewResponse
                    )
            );

            verify(webSocketPublisher)
                    .sendAfterCommit(
                            CHAT_DESTINATION,
                            chatMessageResponse
                    );

            verifyNoInteractions(afterCommitExecutor);
        }
    }

    @Test
    void 일정_공유_취소_메시지를_취소된_상태로_발행한다() {
        // given
        int unreadCount = 5;

        when(chatMessageResponse.roomId())
                .thenReturn(ROOM_ID);

        try (
                MockedStatic<SchedulePreviewResponse> previewMock =
                        mockStatic(SchedulePreviewResponse.class);
                MockedStatic<ChatMessageResponse> responseMock =
                        mockStatic(ChatMessageResponse.class);
                MockedStatic<WebSocketDestination> destinationMock =
                        mockStatic(WebSocketDestination.class)
        ) {
            previewMock.when(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.CANCELED)
                    )
            ).thenReturn(schedulePreviewResponse);

            responseMock.when(() ->
                    ChatMessageResponse.from(
                            chatMessage,
                            unreadCount,
                            schedulePreviewResponse
                    )
            ).thenReturn(chatMessageResponse);

            destinationMock.when(() ->
                    WebSocketDestination.getChatDestination(ROOM_ID)
            ).thenReturn(CHAT_DESTINATION);

            // when
            publisher.publishScheduleShareCanceled(
                    chatMessage,
                    unreadCount
            );

            // then
            previewMock.verify(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.CANCELED)
                    )
            );

            responseMock.verify(() ->
                    ChatMessageResponse.from(
                            chatMessage,
                            unreadCount,
                            schedulePreviewResponse
                    )
            );

            verify(webSocketPublisher)
                    .sendAfterCommit(
                            CHAT_DESTINATION,
                            chatMessageResponse
                    );
        }
    }

    @Test
    void 여러_공유_메시지를_취소된_상태로_발행한다() {
        // given
        ChatMessage firstMessage = mock(ChatMessage.class);
        ChatMessage secondMessage = mock(ChatMessage.class);

        ChatMessageResponse firstResponse = mock(ChatMessageResponse.class);
        ChatMessageResponse secondResponse = mock(ChatMessageResponse.class);

        SchedulePreviewResponse firstPreview =
                mock(SchedulePreviewResponse.class);
        SchedulePreviewResponse secondPreview =
                mock(SchedulePreviewResponse.class);

        when(firstResponse.roomId()).thenReturn(ROOM_ID);
        when(secondResponse.roomId()).thenReturn(2L);

        try (
                MockedStatic<SchedulePreviewResponse> previewMock =
                        mockStatic(SchedulePreviewResponse.class);
                MockedStatic<ChatMessageResponse> responseMock =
                        mockStatic(ChatMessageResponse.class);
                MockedStatic<WebSocketDestination> destinationMock =
                        mockStatic(WebSocketDestination.class)
        ) {
            previewMock.when(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.CANCELED)
                    )
            ).thenReturn(firstPreview, secondPreview);

            responseMock.when(() ->
                    ChatMessageResponse.from(
                            firstMessage,
                            0,
                            firstPreview
                    )
            ).thenReturn(firstResponse);

            responseMock.when(() ->
                    ChatMessageResponse.from(
                            secondMessage,
                            0,
                            secondPreview
                    )
            ).thenReturn(secondResponse);

            destinationMock.when(() ->
                    WebSocketDestination.getChatDestination(ROOM_ID)
            ).thenReturn(CHAT_DESTINATION);

            destinationMock.when(() ->
                    WebSocketDestination.getChatDestination(2L)
            ).thenReturn("/sub/chat/rooms/2");

            ArgumentCaptor<Runnable> runnableCaptor =
                    ArgumentCaptor.forClass(Runnable.class);

            // when
            publisher.publishScheduleShareCanceled(
                    List.of(firstMessage, secondMessage)
            );

            // then
            verify(afterCommitExecutor)
                    .execute(runnableCaptor.capture());

            verifyNoInteractions(webSocketPublisher);

            runnableCaptor.getValue().run();

            verify(webSocketPublisher)
                    .send(CHAT_DESTINATION, firstResponse);

            verify(webSocketPublisher)
                    .send("/sub/chat/rooms/2", secondResponse);
        }
    }

    @Test
    void 여러_공유_메시지를_삭제된_상태로_발행한다() {
        // given
        ChatMessage firstMessage = mock(ChatMessage.class);
        ChatMessageResponse firstResponse = mock(ChatMessageResponse.class);
        SchedulePreviewResponse firstPreview =
                mock(SchedulePreviewResponse.class);

        when(firstResponse.roomId()).thenReturn(ROOM_ID);

        try (
                MockedStatic<SchedulePreviewResponse> previewMock =
                        mockStatic(SchedulePreviewResponse.class);
                MockedStatic<ChatMessageResponse> responseMock =
                        mockStatic(ChatMessageResponse.class);
                MockedStatic<WebSocketDestination> destinationMock =
                        mockStatic(WebSocketDestination.class)
        ) {
            previewMock.when(() ->
                    SchedulePreviewResponse.from(
                            anyLong(),
                            anyLong(),
                            any(),
                            eq(SchedulePreviewState.DELETED)
                    )
            ).thenReturn(firstPreview);

            responseMock.when(() ->
                    ChatMessageResponse.from(
                            firstMessage,
                            0,
                            firstPreview
                    )
            ).thenReturn(firstResponse);

            destinationMock.when(() ->
                    WebSocketDestination.getChatDestination(ROOM_ID)
            ).thenReturn(CHAT_DESTINATION);

            ArgumentCaptor<Runnable> runnableCaptor =
                    ArgumentCaptor.forClass(Runnable.class);

            // when
            publisher.publishSharedScheduleDeleted(
                    List.of(firstMessage)
            );

            // then
            verify(afterCommitExecutor)
                    .execute(runnableCaptor.capture());

            verifyNoInteractions(webSocketPublisher);

            runnableCaptor.getValue().run();

            verify(webSocketPublisher)
                    .send(CHAT_DESTINATION, firstResponse);
        }
    }

    @Test
    void 빈_메시지_목록은_웹소켓을_발행하지_않는다() {
        // when
        publisher.publishScheduleUpdated(List.of());

        // then
        verifyNoInteractions(
                afterCommitExecutor,
                webSocketPublisher
        );
    }

    @Test
    void 빈_공유_취소_메시지_목록은_웹소켓을_발행하지_않는다() {
        // when
        publisher.publishScheduleShareCanceled(List.of());

        // then
        verifyNoInteractions(
                afterCommitExecutor,
                webSocketPublisher
        );
    }

    @Test
    void 빈_일정_삭제_메시지_목록은_웹소켓을_발행하지_않는다() {
        // when
        publisher.publishSharedScheduleDeleted(List.of());

        // then
        verifyNoInteractions(
                afterCommitExecutor,
                webSocketPublisher
        );
    }
}