package com.example.schedulebook.domain.chatmessage.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageScheduleShareRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageSearchRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageSendRequest;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageSliceResponse;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatmessage.publisher.ChatMessagePublisher;
import com.example.schedulebook.domain.chatmessage.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomRepository;
import com.example.schedulebook.domain.chatroom.validator.ChatRoomValidator;
import com.example.schedulebook.domain.chatmessage.validator.ChatMessageValidator;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.validator.ScheduleValidator;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleparticipant.publisher.ScheduleParticipantPublisher;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.scheduleparticipant.validator.ScheduleParticipantValidator;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.publisher.ScheduleSharePublisher;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotDiffResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotHistoryResponse;
import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshot;
import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshotHistory;
import com.example.schedulebook.domain.schedulesnapshot.repository.ScheduleSnapshotHistoryRepository;
import com.example.schedulebook.domain.schedulesnapshot.service.ScheduleSnapshotComparator;
import com.example.schedulebook.domain.schedulesnapshot.service.ScheduleSnapshotHistoryManager;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ROOM_ID = 10L;
    private static final Long MESSAGE_ID = 100L;
    private static final Long REPLY_MESSAGE_ID = 101L;
    private static final Long SCHEDULE_ID = 200L;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ScheduleSharePublisher scheduleSharePublisher;

    @Mock
    private ChatMessagePublisher chatMessagePublisher;

    @Mock
    private ScheduleSnapshotHistoryRepository scheduleSnapshotHistoryRepository;

    @Mock
    private ScheduleSnapshotComparator scheduleSnapshotComparator;

    @Mock
    private ScheduleSnapshotHistoryManager scheduleSnapshotHistoryManager;

    @Mock
    private ChatUnreadCountManager chatUnreadCountManager;

    @Mock
    private ScheduleShareRepository scheduleShareRepository;

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    @Mock
    private ScheduleParticipantPublisher scheduleParticipantPublisher;

    @Mock
    private UserValidator userValidator;

    @Mock
    private ScheduleValidator scheduleValidator;

    @Mock
    private ChatRoomValidator chatRoomValidator;

    @Mock
    private ChatMessageValidator chatMessageValidator;

    @Mock
    private ScheduleParticipantValidator scheduleParticipantValidator;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    private ChatMessageService chatMessageService;

    private ChatRoom chatRoom;
    private User sender;
    private ChatRoomMember chatRoomMember;

    @BeforeEach
    void setUp() {
        chatMessageService = new ChatMessageService(
                chatRoomMemberRepository,
                chatMessageRepository,
                scheduleSharePublisher,
                chatMessagePublisher,
                scheduleSnapshotHistoryRepository,
                scheduleSnapshotComparator,
                scheduleSnapshotHistoryManager,
                chatUnreadCountManager,
                scheduleShareRepository,
                scheduleParticipantRepository,
                scheduleParticipantPublisher,
                userValidator,
                scheduleValidator,
                chatRoomValidator,
                chatMessageValidator,
                scheduleParticipantValidator,
                chatRoomRepository
        );

        chatRoom = mock(ChatRoom.class);
        sender = mock(User.class);
        chatRoomMember = mock(ChatRoomMember.class);

        lenient().when(chatRoom.getId()).thenReturn(ROOM_ID);
        lenient().when(chatRoom.getChatRoomType()).thenReturn(ChatRoomType.DIRECT);

        lenient().when(sender.getId()).thenReturn(USER_ID);
        lenient().when(sender.getNickname()).thenReturn("테스트 사용자");

        lenient().when(chatRoomMember.getJoinedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
        lenient().when(chatRoomMember.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatRoomMember.getUser()).thenReturn(sender);
        lenient().when(chatRoomMember.getLastReadMessageId()).thenReturn(90L);
    }

    @Test
    void sendMessage_일반_메시지를_저장하고_발행한다() {
        // given
        ChatMessageSendRequest request =
                new ChatMessageSendRequest(ROOM_ID, "안녕하세요", null);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateMember(ROOM_ID, USER_ID))
                .thenReturn(sender);
        when(chatMessageValidator.validateContent("안녕하세요"))
                .thenReturn("안녕하세요");
        when(chatMessageValidator.validateReplyMessage(null, ROOM_ID))
                .thenReturn(null);
        when(chatUnreadCountManager.calculateUnreadCount(
                any(ChatMessage.class),
                anyList()
        )).thenReturn(2);
        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        chatMessageService.sendMessage(USER_ID, request);

        // then
        ArgumentCaptor<ChatMessage> captor =
                ArgumentCaptor.forClass(ChatMessage.class);

        verify(chatMessageRepository).save(captor.capture());
        verify(chatMessagePublisher).publishMessage(captor.getValue(), 2);

        ChatMessage savedMessage = captor.getValue();

        assertThat(savedMessage.getChatRoom()).isSameAs(chatRoom);
        assertThat(savedMessage.getSender()).isSameAs(sender);
        assertThat(savedMessage.getContent()).isEqualTo("안녕하세요");
        assertThat(savedMessage.getChatMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(savedMessage.getReplyMessage()).isNull();

        verify(chatRoom).updateLastMessage(savedMessage);
        verify(chatRoomMemberRepository).increaseUnreadCount(ROOM_ID, USER_ID);
    }

    @Test
    void sendMessage_reply_메시지를_저장한다() {
        // given
        ChatMessage replyMessage = mock(ChatMessage.class);

        ChatMessageSendRequest request =
                new ChatMessageSendRequest(
                        ROOM_ID,
                        "답장입니다.",
                        REPLY_MESSAGE_ID
                );

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateMember(ROOM_ID, USER_ID))
                .thenReturn(sender);
        when(chatMessageValidator.validateContent("답장입니다."))
                .thenReturn("답장입니다.");
        when(chatMessageValidator.validateReplyMessage(
                REPLY_MESSAGE_ID,
                ROOM_ID
        )).thenReturn(replyMessage);

        when(chatUnreadCountManager.calculateUnreadCount(
                any(ChatMessage.class),
                anyList()
        )).thenReturn(1);
        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        chatMessageService.sendMessage(USER_ID, request);

        // then
        ArgumentCaptor<ChatMessage> captor =
                ArgumentCaptor.forClass(ChatMessage.class);

        verify(chatMessageRepository).save(captor.capture());

        assertThat(captor.getValue().getReplyMessage())
                .isSameAs(replyMessage);

        verify(chatMessagePublisher)
                .publishMessage(captor.getValue(), 1);
    }

    @Test
    void sendMessage_content_검증에_실패하면_메시지를_저장하지_않는다() {
        // given
        ChatMessageSendRequest request =
                new ChatMessageSendRequest(ROOM_ID, "잘못된 메시지", null);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateMember(ROOM_ID, USER_ID))
                .thenReturn(sender);

        when(chatMessageValidator.validateContent("잘못된 메시지"))
                .thenThrow(new BaseException(ErrorEnum.INVALID_INPUT));

        // when & then
        assertThatThrownBy(() ->
                chatMessageService.sendMessage(USER_ID, request)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        verify(chatMessageRepository, never()).save(any());
        verify(chatMessagePublisher, never())
                .publishMessage(any(), anyInt());
        verify(chatMessageValidator, never())
                .validateReplyMessage(any(), any());
    }

    @Test
    void sendMessage_reply_검증에_실패하면_메시지를_저장하지_않는다() {
        // given
        ChatMessageSendRequest request =
                new ChatMessageSendRequest(
                        ROOM_ID,
                        "답장",
                        REPLY_MESSAGE_ID
                );

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateMember(ROOM_ID, USER_ID))
                .thenReturn(sender);
        when(chatMessageValidator.validateContent("답장"))
                .thenReturn("답장");

        when(chatMessageValidator.validateReplyMessage(
                REPLY_MESSAGE_ID,
                ROOM_ID
        )).thenThrow(new BaseException(ErrorEnum.CHAT_MESSAGE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() ->
                chatMessageService.sendMessage(USER_ID, request)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_NOT_FOUND);

        verify(chatMessageRepository, never()).save(any());
        verify(chatMessagePublisher, never())
                .publishMessage(any(), anyInt());
    }

    @Test
    void findMessages_기본_페이지_크기_30을_사용한다() {
        // given
        ChatMessageSearchRequest request =
                new ChatMessageSearchRequest(null, null);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenReturn(chatRoomMember);

        when(chatMessageRepository.findMessages(
                eq(ROOM_ID),
                any(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of());

        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        ChatMessageSliceResponse response =
                chatMessageService.findMessages(USER_ID, ROOM_ID, request);

        // then
        assertThat(response.messages()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(chatMessageRepository).findMessages(
                eq(ROOM_ID),
                any(),
                isNull(),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageSize())
                .isEqualTo(31);
    }

    @Test
    void findMessages_size가_0이하이면_기본_페이지_크기_30을_사용한다() {
        // given
        ChatMessageSearchRequest request =
                new ChatMessageSearchRequest(null, 0);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenReturn(chatRoomMember);
        when(chatMessageRepository.findMessages(
                eq(ROOM_ID),
                any(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        chatMessageService.findMessages(USER_ID, ROOM_ID, request);

        // then
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(chatMessageRepository).findMessages(
                eq(ROOM_ID),
                any(),
                isNull(),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageSize())
                .isEqualTo(31);
    }

    @Test
    void findMessages_다음_페이지가_있으면_마지막_메시지를_제거하고_cursor를_반환한다() {
        // given
        ChatMessageSearchRequest request =
                new ChatMessageSearchRequest(null, 2);

        ChatMessage first = createMessage(1L);
        ChatMessage second = createMessage(2L);
        ChatMessage third = createMessage(3L);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenReturn(chatRoomMember);

        when(chatMessageRepository.findMessages(
                eq(ROOM_ID),
                any(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(new ArrayList<>(List.of(first, second, third)));

        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        when(chatUnreadCountManager.calculateUnreadCount(
                any(ChatMessage.class),
                anyList()
        )).thenReturn(0);

        // when
        ChatMessageSliceResponse response =
                chatMessageService.findMessages(USER_ID, ROOM_ID, request);

        // then
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(2L);
        assertThat(response.messages())
                .extracting(ChatMessageResponse::messageId)
                .containsExactly(1L, 2L);

        verify(chatUnreadCountManager, times(2))
                .calculateUnreadCount(any(ChatMessage.class), anyList());
    }

    @Test
    void findMessages_다음_페이지가_없으면_cursor는_null이다() {
        // given
        ChatMessageSearchRequest request =
                new ChatMessageSearchRequest(null, 2);

        ChatMessage first = createMessage(1L);
        ChatMessage second = createMessage(2L);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenReturn(chatRoomMember);
        when(chatMessageRepository.findMessages(
                eq(ROOM_ID),
                any(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());
        when(chatUnreadCountManager.calculateUnreadCount(
                any(ChatMessage.class),
                anyList()
        )).thenReturn(0);

        // when
        ChatMessageSliceResponse response =
                chatMessageService.findMessages(USER_ID, ROOM_ID, request);

        // then
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.messages())
                .extracting(ChatMessageResponse::messageId)
                .containsExactly(1L, 2L);
    }

    @Test
    void readMessage_읽음_처리와_unreadCount_재계산_후_이벤트를_발행한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenReturn(chatRoomMember);

        when(chatMessageValidator.validateChatMessageInRoom(
                MESSAGE_ID,
                ROOM_ID
        )).thenReturn(chatMessage);

        when(chatUnreadCountManager.recalculateUnreadCount(chatRoomMember))
                .thenReturn(3L);

        doAnswer(invocation -> {
            when(chatRoomMember.getLastReadMessageId())
                    .thenReturn(invocation.getArgument(0));
            return null;
        }).when(chatRoomMember).updateLastRead(MESSAGE_ID);

        // when
        chatMessageService.readMessage(USER_ID, ROOM_ID, MESSAGE_ID);

        // then
        verify(chatRoomMember).updateLastRead(MESSAGE_ID);
        verify(chatUnreadCountManager).recalculateUnreadCount(chatRoomMember);
        verify(chatRoomMember).updateUnreadCount(3);

        verify(chatMessagePublisher)
                .publishReadMessageAfterCommit(
                        ROOM_ID,
                        USER_ID,
                        MESSAGE_ID
                );
    }

    @Test
    void readMessage_채팅방_멤버가_아니면_예외가_발생한다() {
        // given
        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenThrow(new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN));

        // when & then
        assertThatThrownBy(() ->
                chatMessageService.readMessage(USER_ID, ROOM_ID, MESSAGE_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN);

        verify(chatMessageValidator, never())
                .validateChatMessageInRoom(anyLong(), anyLong());

        verify(chatMessagePublisher, never())
                .publishReadMessageAfterCommit(anyLong(), anyLong(), anyLong());
    }

    @Test
    void readMessage_읽을_수_없는_메시지면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenReturn(chatRoomMember);
        when(chatMessageValidator.validateChatMessageInRoom(
                MESSAGE_ID,
                ROOM_ID
        )).thenReturn(chatMessage);

        doThrow(new BaseException(ErrorEnum.CHAT_MESSAGE_FORBIDDEN))
                .when(chatMessageValidator)
                .validateReadableMessage(chatRoomMember, chatMessage);

        // when & then
        assertThatThrownBy(() ->
                chatMessageService.readMessage(USER_ID, ROOM_ID, MESSAGE_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_FORBIDDEN);

        verify(chatRoomMember, never()).updateLastRead(anyLong());
        verify(chatMessagePublisher, never())
                .publishReadMessageAfterCommit(anyLong(), anyLong(), anyLong());
    }

    @Test
    void deleteMessage_메시지를_삭제하고_삭제_이벤트를_발행한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenReturn(chatRoomMember);
        when(chatMessageValidator.validateChatMessageInRoom(
                MESSAGE_ID,
                ROOM_ID
        )).thenReturn(chatMessage);

        // when
        chatMessageService.deleteMessage(USER_ID, ROOM_ID, MESSAGE_ID);

        // then
        verify(chatMessageValidator).validateDeleteMessage(chatMessage);
        verify(chatMessage).deleteMessage(USER_ID);
        verify(chatMessagePublisher)
                .publishDeleteMessageAfterCommit(ROOM_ID, MESSAGE_ID);
    }

    @Test
    void deleteMessage_채팅방_멤버가_아니면_메시지를_삭제하지_않는다() {
        // given
        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenThrow(new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN));

        // when & then
        assertThatThrownBy(() ->
                chatMessageService.deleteMessage(USER_ID, ROOM_ID, MESSAGE_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN);

        verify(chatMessageValidator, never())
                .validateChatMessageInRoom(anyLong(), anyLong());
        verify(chatMessagePublisher, never())
                .publishDeleteMessageAfterCommit(anyLong(), anyLong());
    }

    @Test
    void deleteMessage_삭제_권한이_없으면_삭제하지_않는다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID))
                .thenReturn(chatRoomMember);
        when(chatMessageValidator.validateChatMessageInRoom(
                MESSAGE_ID,
                ROOM_ID
        )).thenReturn(chatMessage);

        doThrow(new BaseException(ErrorEnum.CHAT_MESSAGE_FORBIDDEN))
                .when(chatMessageValidator)
                .validateDeleteMessage(chatMessage);

        // when & then
        assertThatThrownBy(() ->
                chatMessageService.deleteMessage(USER_ID, ROOM_ID, MESSAGE_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_FORBIDDEN);

        verify(chatMessage, never()).deleteMessage(anyLong());
        verify(chatMessagePublisher, never())
                .publishDeleteMessageAfterCommit(anyLong(), anyLong());
    }

    @Test
    void shareSchedule_일정_메시지를_저장하고_공유_이벤트를_발행한다() {
        // given
        ChatMessageScheduleShareRequest request =
                new ChatMessageScheduleShareRequest(ROOM_ID, SCHEDULE_ID);

        Schedule schedule = mock(Schedule.class);

        when(schedule.getId()).thenReturn(SCHEDULE_ID);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);
        when(chatRoomValidator.validateMember(ROOM_ID, USER_ID))
                .thenReturn(sender);
        when(scheduleValidator.validateSchedule(SCHEDULE_ID, USER_ID))
                .thenReturn(schedule);

        when(chatUnreadCountManager.calculateUnreadCount(
                any(ChatMessage.class),
                anyList()
        )).thenReturn(2);
        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        chatMessageService.shareSchedule(USER_ID, request);

        // then
        ArgumentCaptor<ChatMessage> captor =
                ArgumentCaptor.forClass(ChatMessage.class);

        verify(chatMessageRepository).save(captor.capture());

        ChatMessage savedMessage = captor.getValue();

        assertThat(savedMessage.getChatRoom()).isSameAs(chatRoom);
        assertThat(savedMessage.getSender()).isSameAs(sender);
        assertThat(savedMessage.getScheduleId()).isEqualTo(SCHEDULE_ID);
        assertThat(savedMessage.getChatMessageType())
                .isEqualTo(ChatMessageType.SCHEDULE);
        assertThat(savedMessage.getScheduleSnapshot())
                .isNotNull();

        verify(scheduleSharePublisher)
                .publishScheduleShared(savedMessage, 2);
    }

    @Test
    void acceptSharedSchedule_공유를_저장하고_참여자를_생성한_후_이벤트를_발행한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        Schedule schedule = mock(Schedule.class);
        User currentUser = mock(User.class);

        when(chatMessageValidator.validateReadableScheduleMessage(
                USER_ID,
                MESSAGE_ID
        )).thenReturn(chatMessage);
        when(scheduleValidator.findSchedule(
                chatMessage.getScheduleId()
        )).thenReturn(schedule);
        when(schedule.getUser()).thenReturn(sender);
        when(sender.getId()).thenReturn(OTHER_USER_ID);
        when(schedule.getId()).thenReturn(SCHEDULE_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        // when
        chatMessageService.acceptSharedSchedule(USER_ID, MESSAGE_ID);

        // then
        verify(userValidator)
                .validateShareMyself(USER_ID, OTHER_USER_ID);

        verify(scheduleParticipantValidator)
                .validateAlreadyParticipated(SCHEDULE_ID, USER_ID);

        ArgumentCaptor<ScheduleShare> shareCaptor =
                ArgumentCaptor.forClass(ScheduleShare.class);

        verify(scheduleShareRepository).save(shareCaptor.capture());

        assertThat(shareCaptor.getValue().getSchedule())
                .isSameAs(schedule);
        assertThat(shareCaptor.getValue().getSharedUser())
                .isSameAs(currentUser);
        assertThat(shareCaptor.getValue().getScheduleShareStatus())
                .isEqualTo(ScheduleShareStatus.ACTIVE);

        ArgumentCaptor<ScheduleParticipant> participantCaptor =
                ArgumentCaptor.forClass(ScheduleParticipant.class);

        verify(scheduleParticipantRepository)
                .save(participantCaptor.capture());

        assertThat(participantCaptor.getValue().getSchedule())
                .isSameAs(schedule);
        assertThat(participantCaptor.getValue().getUser())
                .isSameAs(currentUser);

        verify(scheduleSharePublisher)
                .publishAcceptSharedSchedule(chatMessage);
        verify(scheduleParticipantPublisher)
                .publishParticipantsUpdated(SCHEDULE_ID);
    }

    @Test
    void acceptSharedSchedule_이미_참여한_사용자면_공유를_저장하지_않는다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        Schedule schedule = mock(Schedule.class);

        when(chatMessageValidator.validateReadableScheduleMessage(
                USER_ID,
                MESSAGE_ID
        )).thenReturn(chatMessage);
        when(scheduleValidator.findSchedule(
                chatMessage.getScheduleId()
        )).thenReturn(schedule);
        when(schedule.getUser()).thenReturn(sender);
        when(sender.getId()).thenReturn(OTHER_USER_ID);
        when(schedule.getId()).thenReturn(SCHEDULE_ID);

        doThrow(new BaseException(ErrorEnum.SCHEDULE_ALREADY_PARTICIPATED))
                .when(scheduleParticipantValidator)
                .validateAlreadyParticipated(SCHEDULE_ID, USER_ID);

        // when & then
        assertThatThrownBy(() ->
                chatMessageService.acceptSharedSchedule(USER_ID, MESSAGE_ID)
        )
                .isInstanceOf(BaseException.class);

        verify(scheduleShareRepository, never()).save(any());
        verify(scheduleParticipantRepository, never()).save(any());
        verify(scheduleSharePublisher, never())
                .publishAcceptSharedSchedule(any());
    }

    @Test
    void cancelScheduleShare_공유를_취소하고_이벤트를_발행한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessageValidator.validateChatMessage(MESSAGE_ID))
                .thenReturn(chatMessage);

        when(chatMessage.getChatRoom()).thenReturn(chatRoom);

        when(chatUnreadCountManager.calculateUnreadCount(
                any(ChatMessage.class),
                anyList()
        )).thenReturn(3);
        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        chatMessageService.cancelScheduleShare(USER_ID, MESSAGE_ID);

        // then
        verify(chatMessageValidator)
                .validateScheduleMessageType(chatMessage);

        verify(chatMessage)
                .cancelScheduleShare(USER_ID);

        verify(scheduleSharePublisher)
                .publishScheduleShareCanceled(chatMessage, 3);
    }

    @Test
    void findSharedSchedule_공유_일정_상태를_반환한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ScheduleSnapshot snapshot = mock(ScheduleSnapshot.class);

        when(chatMessageValidator.validateReadableScheduleMessage(
                USER_ID,
                MESSAGE_ID
        )).thenReturn(chatMessage);

        when(chatMessage.getScheduleId())
                .thenReturn(SCHEDULE_ID);

        when(chatMessage.getScheduleSnapshot())
                .thenReturn(snapshot);

        when(snapshot.getTitle())
                .thenReturn("테스트 일정");

        when(snapshot.getContent())
                .thenReturn("테스트 내용");

        when(snapshot.getScheduleDate())
                .thenReturn(LocalDate.of(2026, 9, 30));

        when(snapshot.getStartTime())
                .thenReturn(LocalTime.of(10, 0));

        when(snapshot.getEndTime())
                .thenReturn(LocalTime.of(11, 0));

        when(snapshot.getScheduleVersion())
                .thenReturn(1L);

        when(snapshot.getScheduleUpdatedAt())
                .thenReturn(LocalDateTime.of(2026, 9, 30, 9, 0));

        when(scheduleParticipantValidator.isAlreadyScheduleShared(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(true);

        // when
        var response =
                chatMessageService.findSharedSchedule(USER_ID, MESSAGE_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.shared()).isTrue();
    }

    @Test
    void findScheduleSnapshotHistory_스냅샷_이력을_조회하고_응답으로_변환한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ScheduleSnapshotHistory history =
                mock(ScheduleSnapshotHistory.class);
        ScheduleSnapshot snapshot =
                mock(ScheduleSnapshot.class);

        when(chatMessageValidator.validateReadableScheduleMessage(
                USER_ID,
                MESSAGE_ID
        )).thenReturn(chatMessage);

        when(chatMessage.getId()).thenReturn(MESSAGE_ID);
        when(scheduleSnapshotHistoryRepository.findAllByChatMessageId(
                MESSAGE_ID
        )).thenReturn(List.of(history));

        when(history.getScheduleSnapshot())
                .thenReturn(snapshot);

        when(snapshot.getTitle()).thenReturn("테스트 일정");
        when(snapshot.getContent()).thenReturn("내용");
        when(snapshot.getScheduleVersion()).thenReturn(1L);

        // when
        List<ScheduleSnapshotHistoryResponse> responses =
                chatMessageService.findScheduleSnapshotHistory(
                        USER_ID,
                        MESSAGE_ID
                );

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title())
                .isEqualTo("테스트 일정");
        assertThat(responses.get(0).content())
                .isEqualTo("내용");
        assertThat(responses.get(0).scheduleVersion())
                .isEqualTo(1L);
    }

    @Test
    void findScheduleSnapshotDiff_두_버전의_스냅샷을_비교한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ScheduleSnapshot before = mock(ScheduleSnapshot.class);
        ScheduleSnapshot after = mock(ScheduleSnapshot.class);
        ScheduleSnapshotDiffResponse diffResponse =
                mock(ScheduleSnapshotDiffResponse.class);

        when(chatMessageValidator.validateReadableScheduleMessage(
                USER_ID,
                MESSAGE_ID
        )).thenReturn(chatMessage);

        when(scheduleSnapshotHistoryManager.findSnapshot(
                chatMessage,
                1L
        )).thenReturn(before);

        when(scheduleSnapshotHistoryManager.findSnapshot(
                chatMessage,
                2L
        )).thenReturn(after);

        when(scheduleSnapshotComparator.compare(before, after))
                .thenReturn(diffResponse);

        // when
        ScheduleSnapshotDiffResponse response =
                chatMessageService.findScheduleSnapshotDiff(
                        USER_ID,
                        MESSAGE_ID,
                        1L,
                        2L
                );

        // then
        assertThat(response).isSameAs(diffResponse);

        verify(scheduleSnapshotHistoryManager)
                .findSnapshot(chatMessage, 1L);
        verify(scheduleSnapshotHistoryManager)
                .findSnapshot(chatMessage, 2L);
        verify(scheduleSnapshotComparator)
                .compare(before, after);
    }

    private ChatMessage createMessage(Long id) {
        ChatMessage message = mock(ChatMessage.class);

        lenient().when(message.getId()).thenReturn(id);
        lenient().when(message.getChatRoom()).thenReturn(chatRoom);
        lenient().when(message.getSender()).thenReturn(sender);
        lenient().when(message.getCreatedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));

        return message;
    }
}