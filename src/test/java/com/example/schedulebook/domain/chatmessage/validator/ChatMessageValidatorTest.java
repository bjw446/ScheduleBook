package com.example.schedulebook.domain.chatmessage.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatmessage.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.validator.ChatRoomValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageValidatorTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;
    private static final Long MESSAGE_ID = 100L;
    private static final Long REPLY_MESSAGE_ID = 101L;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomValidator chatRoomValidator;

    private ChatMessageValidator chatMessageValidator;

    @BeforeEach
    void setUp() {
        chatMessageValidator = new ChatMessageValidator(
                chatMessageRepository,
                chatRoomValidator
        );
    }

    @Test
    void validateScheduleMessageType_일정_메시지이면_통과한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessage.getChatMessageType())
                .thenReturn(ChatMessageType.SCHEDULE);

        // when & then
        assertThatCode(() ->
                chatMessageValidator.validateScheduleMessageType(chatMessage)
        ).doesNotThrowAnyException();
    }

    @Test
    void validateScheduleMessageType_일반_메시지이면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessage.getChatMessageType())
                .thenReturn(ChatMessageType.TEXT);

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateScheduleMessageType(chatMessage)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_MESSAGE_TYPE);
    }

    @Test
    void validateDeleteMessage_삭제되지_않은_최근_메시지이면_통과한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessage.isDeleted()).thenReturn(false);
        when(chatMessage.getCreatedAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        // when & then
        assertThatCode(() ->
                chatMessageValidator.validateDeleteMessage(chatMessage)
        ).doesNotThrowAnyException();
    }

    @Test
    void validateDeleteMessage_이미_삭제된_메시지이면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessage.isDeleted()).thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateDeleteMessage(chatMessage)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_ALREADY_DELETE);

        verify(chatMessage, never()).getCreatedAt();
    }

    @Test
    void validateDeleteMessage_5분이_지난_메시지이면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessage.isDeleted()).thenReturn(false);
        when(chatMessage.getCreatedAt())
                .thenReturn(LocalDateTime.now().minusMinutes(6));

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateDeleteMessage(chatMessage)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_DELETE_NOT_ALLOWED);
    }

    @Test
    void validateContent_정상적인_내용이면_trim_후_반환한다() {
        // given
        String content = "   안녕하세요   ";

        // when
        String result = chatMessageValidator.validateContent(content);

        // then
        assertThat(result).isEqualTo("안녕하세요");
    }

    @Test
    void validateContent_null이면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateContent(null)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_EMPTY);
    }

    @Test
    void validateContent_빈문자열이면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateContent("")
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_EMPTY);
    }

    @Test
    void validateContent_공백만_있는_문자열이면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateContent("     ")
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_EMPTY);
    }

    @Test
    void validateContent_1000자를_초과하면_예외가_발생한다() {
        // given
        String content = "a".repeat(1001);

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateContent(content)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_TOO_LONG);
    }

    @Test
    void validateContent_1000자이면_통과한다() {
        // given
        String content = "a".repeat(1000);

        // when
        String result = chatMessageValidator.validateContent(content);

        // then
        assertThat(result).hasSize(1000);
    }

    @Test
    void validateReplyMessage_replyMessageId가_null이면_null을_반환한다() {
        // when
        ChatMessage result =
                chatMessageValidator.validateReplyMessage(null, ROOM_ID);

        // then
        assertThat(result).isNull();

        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void validateReplyMessage_같은_채팅방의_메시지이면_메시지를_반환한다() {
        // given
        ChatMessage replyMessage = mock(ChatMessage.class);

        when(chatMessageRepository.findByIdAndChatRoomId(
                REPLY_MESSAGE_ID,
                ROOM_ID
        )).thenReturn(Optional.of(replyMessage));

        // when
        ChatMessage result =
                chatMessageValidator.validateReplyMessage(
                        REPLY_MESSAGE_ID,
                        ROOM_ID
                );

        // then
        assertThat(result).isSameAs(replyMessage);

        verify(chatMessageRepository)
                .findByIdAndChatRoomId(REPLY_MESSAGE_ID, ROOM_ID);
    }

    @Test
    void validateReplyMessage_다른_채팅방이거나_존재하지_않으면_예외가_발생한다() {
        // given
        when(chatMessageRepository.findByIdAndChatRoomId(
                REPLY_MESSAGE_ID,
                ROOM_ID
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateReplyMessage(
                        REPLY_MESSAGE_ID,
                        ROOM_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_REPLY_MESSAGE);
    }

    @Test
    void validateChatMessageInRoom_messageId가_null이면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateChatMessageInRoom(
                        null,
                        ROOM_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void validateChatMessageInRoom_채팅방의_메시지가_존재하면_메시지를_반환한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessageRepository.findByIdAndChatRoomId(
                MESSAGE_ID,
                ROOM_ID
        )).thenReturn(Optional.of(chatMessage));

        // when
        ChatMessage result =
                chatMessageValidator.validateChatMessageInRoom(
                        MESSAGE_ID,
                        ROOM_ID
                );

        // then
        assertThat(result).isSameAs(chatMessage);
    }

    @Test
    void validateChatMessageInRoom_메시지가_존재하지_않으면_예외가_발생한다() {
        // given
        when(chatMessageRepository.findByIdAndChatRoomId(
                MESSAGE_ID,
                ROOM_ID
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateChatMessageInRoom(
                        MESSAGE_ID,
                        ROOM_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_NOT_FOUND);
    }

    @Test
    void validateChatMessage_messageId가_null이면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateChatMessage(null)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void validateChatMessage_메시지가_존재하면_메시지를_반환한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessageRepository.findByIdWithChatRoom(MESSAGE_ID))
                .thenReturn(Optional.of(chatMessage));

        // when
        ChatMessage result =
                chatMessageValidator.validateChatMessage(MESSAGE_ID);

        // then
        assertThat(result).isSameAs(chatMessage);
    }

    @Test
    void validateChatMessage_메시지가_존재하지_않으면_예외가_발생한다() {
        // given
        when(chatMessageRepository.findByIdWithChatRoom(MESSAGE_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateChatMessage(MESSAGE_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_NOT_FOUND);
    }

    @Test
    void validateReadableMessage_입장_이전_메시지이면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ChatRoomMember chatRoomMember = mock(ChatRoomMember.class);

        when(chatMessage.getCreatedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 9, 59));

        when(chatRoomMember.getJoinedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateReadableMessage(
                        chatRoomMember,
                        chatMessage
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_MESSAGE_FORBIDDEN);
    }

    @Test
    void validateReadableMessage_입장_이후_메시지이면_통과한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ChatRoomMember chatRoomMember = mock(ChatRoomMember.class);

        when(chatMessage.getCreatedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 1));

        when(chatRoomMember.getJoinedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));

        // when & then
        assertThatCode(() ->
                chatMessageValidator.validateReadableMessage(
                        chatRoomMember,
                        chatMessage
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void validateReadableSharedSchedule_취소된_공유_일정이면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessage.isScheduleShareCanceled())
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateReadableSharedSchedule(chatMessage)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_SHARE_CANCELED);
    }

    @Test
    void validateReadableSharedSchedule_취소되지_않은_공유_일정이면_통과한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(chatMessage.isScheduleShareCanceled())
                .thenReturn(false);

        // when & then
        assertThatCode(() ->
                chatMessageValidator.validateReadableSharedSchedule(chatMessage)
        ).doesNotThrowAnyException();
    }

    @Test
    void validateReadableScheduleMessage_정상적인_공유_일정_메시지를_반환한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        ChatRoomMember chatRoomMember = mock(ChatRoomMember.class);

        when(chatMessageRepository.findByIdWithChatRoom(MESSAGE_ID))
                .thenReturn(Optional.of(chatMessage));

        when(chatMessage.getChatRoom())
                .thenReturn(chatRoom);

        when(chatRoom.getId())
                .thenReturn(ROOM_ID);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(chatRoomMember);

        when(chatMessage.getCreatedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 1));

        when(chatRoomMember.getJoinedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(chatMessage.getChatMessageType())
                .thenReturn(ChatMessageType.SCHEDULE);

        when(chatMessage.isScheduleShareCanceled())
                .thenReturn(false);

        // when
        ChatMessage result =
                chatMessageValidator.validateReadableScheduleMessage(
                        USER_ID,
                        MESSAGE_ID
                );

        // then
        assertThat(result).isSameAs(chatMessage);

        verify(chatRoomValidator)
                .validateChatRoomMember(USER_ID, ROOM_ID);
    }

    @Test
    void validateReadableScheduleMessage_채팅방_멤버가_아니면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ChatRoom chatRoom = mock(ChatRoom.class);

        when(chatMessageRepository.findByIdWithChatRoom(MESSAGE_ID))
                .thenReturn(Optional.of(chatMessage));

        when(chatMessage.getChatRoom())
                .thenReturn(chatRoom);

        when(chatRoom.getId())
                .thenReturn(ROOM_ID);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenThrow(new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN));

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateReadableScheduleMessage(
                        USER_ID,
                        MESSAGE_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN);

        verify(chatMessage, never()).getCreatedAt();
        verify(chatMessage, never()).getChatMessageType();
        verify(chatMessage, never()).isScheduleShareCanceled();
    }

    @Test
    void validateReadableScheduleMessage_일정_메시지가_아니면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        ChatRoomMember chatRoomMember = mock(ChatRoomMember.class);

        when(chatMessageRepository.findByIdWithChatRoom(MESSAGE_ID))
                .thenReturn(Optional.of(chatMessage));

        when(chatMessage.getChatRoom())
                .thenReturn(chatRoom);

        when(chatRoom.getId())
                .thenReturn(ROOM_ID);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(chatRoomMember);

        when(chatMessage.getCreatedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 1));

        when(chatRoomMember.getJoinedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(chatMessage.getChatMessageType())
                .thenReturn(ChatMessageType.TEXT);

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateReadableScheduleMessage(
                        USER_ID,
                        MESSAGE_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_MESSAGE_TYPE);

        verify(chatMessage, never()).isScheduleShareCanceled();
    }

    @Test
    void validateReadableScheduleMessage_취소된_공유_일정이면_예외가_발생한다() {
        // given
        ChatMessage chatMessage = mock(ChatMessage.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        ChatRoomMember chatRoomMember = mock(ChatRoomMember.class);

        when(chatMessageRepository.findByIdWithChatRoom(MESSAGE_ID))
                .thenReturn(Optional.of(chatMessage));

        when(chatMessage.getChatRoom())
                .thenReturn(chatRoom);

        when(chatRoom.getId())
                .thenReturn(ROOM_ID);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(chatRoomMember);

        when(chatMessage.getCreatedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 1));

        when(chatRoomMember.getJoinedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(chatMessage.getChatMessageType())
                .thenReturn(ChatMessageType.SCHEDULE);

        when(chatMessage.isScheduleShareCanceled())
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
                chatMessageValidator.validateReadableScheduleMessage(
                        USER_ID,
                        MESSAGE_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_SHARE_CANCELED);
    }
}