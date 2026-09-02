package com.example.schedulebook.common.websocket.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatSubscriptionValidatorTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatRoomMember chatRoomMember;

    private ChatSubscriptionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChatSubscriptionValidator(
                chatRoomMemberRepository
        );
    }

    @Test
    void 채팅방_destination을_지원한다() {
        // given
        String destination = "/topic/chat/1";

        // when
        boolean result = validator.supports(destination);

        // then
        assertEquals(true, result);
    }

    @Test
    void 채팅방이_아닌_destination은_지원하지_않는다() {
        // given
        String destination = "/topic/notification";

        // when
        boolean result = validator.supports(destination);

        // then
        assertEquals(false, result);
    }

    @Test
    void 채팅방_활성_멤버이면_구독을_허용한다() {
        // given
        Long userId = 1L;
        Long roomId = 10L;
        String destination = "/topic/chat/" + roomId;

        given(chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(
                roomId,
                userId
        )).willReturn(Optional.of(chatRoomMember));

        // when & then
        assertDoesNotThrow(
                () -> validator.validate(userId, destination)
        );

        verify(chatRoomMemberRepository)
                .findActiveByChatRoomIdAndUserId(roomId, userId);
    }

    @Test
    void 채팅방_비회원이면_CHAT_ROOM_FORBIDDEN을_던진다() {
        // given
        Long userId = 1L;
        Long roomId = 10L;
        String destination = "/topic/chat/" + roomId;

        given(chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(
                roomId,
                userId
        )).willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> validator.validate(userId, destination)
        );

        assertEquals(
                ErrorEnum.CHAT_ROOM_FORBIDDEN,
                exception.getErrorEnum()
        );

        verify(chatRoomMemberRepository)
                .findActiveByChatRoomIdAndUserId(roomId, userId);
    }

    @Test
    void 잘못된_chat_destination이면_INVALID_INPUT을_던진다() {
        // given
        Long userId = 1L;
        String destination = "/topic/chat/not-a-number";

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> validator.validate(userId, destination)
        );

        assertEquals(
                ErrorEnum.INVALID_INPUT,
                exception.getErrorEnum()
        );

        verify(chatRoomMemberRepository, never())
                .findActiveByChatRoomIdAndUserId(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong()
                );
    }

    @Test
    void chat_destination에서_roomId를_정확하게_파싱한다() {
        // given
        Long userId = 1L;
        Long roomId = 12345L;
        String destination = "/topic/chat/" + roomId;

        given(chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(
                roomId,
                userId
        )).willReturn(Optional.of(chatRoomMember));

        // when
        validator.validate(userId, destination);

        // then
        verify(chatRoomMemberRepository)
                .findActiveByChatRoomIdAndUserId(roomId, userId);
    }
}