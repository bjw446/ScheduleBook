package com.example.schedulebook.domain.chatroom.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomRepository;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatRoomValidatorTest {

    private static final Long USER_ID = 1L;
    private static final Long FRIEND_ID = 2L;
    private static final Long ANOTHER_FRIEND_ID = 3L;
    private static final Long ROOM_ID = 10L;

    private ChatRoomRepository chatRoomRepository;
    private ChatRoomMemberRepository chatRoomMemberRepository;
    private FriendRepository friendRepository;
    private UserRepository userRepository;

    private ChatRoomValidator chatRoomValidator;

    private User friendUser;
    private User anotherFriendUser;

    @BeforeEach
    void setUp() {
        chatRoomRepository = mock(ChatRoomRepository.class);
        chatRoomMemberRepository = mock(ChatRoomMemberRepository.class);
        friendRepository = mock(FriendRepository.class);
        userRepository = mock(UserRepository.class);

        chatRoomValidator = new ChatRoomValidator(
                chatRoomRepository,
                chatRoomMemberRepository,
                friendRepository,
                userRepository
        );

        friendUser = mock(User.class);
        anotherFriendUser = mock(User.class);

        when(friendUser.getId()).thenReturn(FRIEND_ID);
        when(anotherFriendUser.getId()).thenReturn(ANOTHER_FRIEND_ID);
    }

    @Test
    void validateChatRoomMember_활성_채팅방_멤버이면_멤버정보를_반환한다() {
        // given
        ChatRoomMember member = mock(ChatRoomMember.class);

        when(chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(
                ROOM_ID,
                USER_ID
        )).thenReturn(Optional.of(member));

        // when
        ChatRoomMember result =
                chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID);

        // then
        assertThat(result).isSameAs(member);

        verify(chatRoomMemberRepository)
                .findActiveByChatRoomIdAndUserId(ROOM_ID, USER_ID);
    }

    @Test
    void validateChatRoomMember_활성_채팅방_멤버가_아니면_CHAT_ROOM_FORBIDDEN_예외가_발생한다() {
        // given
        when(chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(
                ROOM_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateChatRoomMember(USER_ID, ROOM_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN);
    }

    @Test
    void validateFriend_수락된_친구이면_정상적으로_통과한다() {
        // given
        when(friendRepository.existsAcceptedFriend(
                USER_ID,
                FRIEND_ID,
                FriendStatus.ACCEPTED
        )).thenReturn(true);

        // when & then
        assertThatCode(() ->
                chatRoomValidator.validateFriend(USER_ID, FRIEND_ID)
        ).doesNotThrowAnyException();

        verify(friendRepository)
                .existsAcceptedFriend(
                        USER_ID,
                        FRIEND_ID,
                        FriendStatus.ACCEPTED
                );
    }

    @Test
    void validateFriend_수락된_친구가_아니면_FRIEND_NOT_FOUND_예외가_발생한다() {
        // given
        when(friendRepository.existsAcceptedFriend(
                USER_ID,
                FRIEND_ID,
                FriendStatus.ACCEPTED
        )).thenReturn(false);

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateFriend(USER_ID, FRIEND_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.FRIEND_NOT_FOUND);
    }

    @Test
    void validateMyself_자기_자신을_대상으로_지정하면_INVALID_CHAT_TARGET_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateMyself(USER_ID, USER_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_CHAT_TARGET);
    }

    @Test
    void validateMyself_다른_사용자를_대상으로_지정하면_정상적으로_통과한다() {
        // when & then
        assertThatCode(() ->
                chatRoomValidator.validateMyself(USER_ID, FRIEND_ID)
        ).doesNotThrowAnyException();
    }

    @Test
    void validateChatRoom_존재하는_채팅방이면_채팅방을_반환한다() {
        // given
        ChatRoom chatRoom = mock(ChatRoom.class);

        when(chatRoomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(chatRoom));

        // when
        ChatRoom result =
                chatRoomValidator.validateChatRoom(ROOM_ID);

        // then
        assertThat(result).isSameAs(chatRoom);

        verify(chatRoomRepository)
                .findById(ROOM_ID);
    }

    @Test
    void validateChatRoom_존재하지_않는_채팅방이면_CHAT_ROOM_NOT_FOUND_예외가_발생한다() {
        // given
        when(chatRoomRepository.findById(ROOM_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateChatRoom(ROOM_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    void validateInviteMembers_자기_자신이_포함되어_있으면_INVALID_CHAT_TARGET_예외가_발생한다() {
        // given
        List<Long> memberIds = List.of(
                FRIEND_ID,
                USER_ID
        );

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateInviteMembers(USER_ID, memberIds)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_CHAT_TARGET);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(friendRepository);
    }

    @Test
    void validateInviteMembers_중복된_멤버가_포함되어_있으면_INVALID_INPUT_예외가_발생한다() {
        // given
        List<Long> memberIds = List.of(
                FRIEND_ID,
                FRIEND_ID
        );

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateInviteMembers(USER_ID, memberIds)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(friendRepository);
    }

    @Test
    void validateInviteMembers_모든_사용자가_존재하면_사용자_목록을_반환한다() {
        // given
        List<Long> memberIds = List.of(
                FRIEND_ID,
                ANOTHER_FRIEND_ID
        );

        List<User> users = List.of(
                friendUser,
                anotherFriendUser
        );

        when(userRepository.findAllById(memberIds))
                .thenReturn(users);

        when(friendRepository.countAcceptedFriends(
                USER_ID,
                memberIds,
                FriendStatus.ACCEPTED
        )).thenReturn(2L);

        // when
        List<User> result =
                chatRoomValidator.validateInviteMembers(USER_ID, memberIds);

        // then
        assertThat(result)
                .containsExactly(friendUser, anotherFriendUser);

        verify(userRepository)
                .findAllById(memberIds);

        verify(friendRepository)
                .countAcceptedFriends(
                        USER_ID,
                        memberIds,
                        FriendStatus.ACCEPTED
                );
    }

    @Test
    void validateInviteMembers_존재하지_않는_사용자가_포함되어_있으면_USER_NOT_FOUND_예외가_발생한다() {
        // given
        List<Long> memberIds = List.of(
                FRIEND_ID,
                ANOTHER_FRIEND_ID
        );

        when(userRepository.findAllById(memberIds))
                .thenReturn(List.of(friendUser));

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateInviteMembers(USER_ID, memberIds)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.USER_NOT_FOUND);

        verify(friendRepository, never())
                .countAcceptedFriends(
                        anyLong(),
                        anyList(),
                        any(FriendStatus.class)
                );
    }

    @Test
    void validateInviteMembers_친구가_아닌_사용자가_포함되어_있으면_FRIEND_NOT_FOUND_예외가_발생한다() {
        // given
        List<Long> memberIds = List.of(
                FRIEND_ID,
                ANOTHER_FRIEND_ID
        );

        List<User> users = List.of(
                friendUser,
                anotherFriendUser
        );

        when(userRepository.findAllById(memberIds))
                .thenReturn(users);

        when(friendRepository.countAcceptedFriends(
                USER_ID,
                memberIds,
                FriendStatus.ACCEPTED
        )).thenReturn(1L);

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateInviteMembers(USER_ID, memberIds)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.FRIEND_NOT_FOUND);
    }

    @Test
    void validateChatRoomType_그룹_채팅방이면_정상적으로_통과한다() {
        // given
        ChatRoom chatRoom = ChatRoom.group("스터디방");

        // when & then
        assertThatCode(() ->
                chatRoomValidator.validateChatRoomType(chatRoom)
        ).doesNotThrowAnyException();
    }

    @Test
    void validateChatRoomType_그룹이_아닌_채팅방이면_INVALID_CHAT_ROOM_TYPE_예외가_발생한다() {
        // given
        ChatRoom chatRoom = ChatRoom.direct();

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateChatRoomType(chatRoom)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_CHAT_ROOM_TYPE);
    }

    @Test
    void validateUpdateName_기존_이름과_다른_이름이면_정상적으로_통과한다() {
        // given
        ChatRoom chatRoom = ChatRoom.group("기존 이름");

        // when & then
        assertThatCode(() ->
                chatRoomValidator.validateUpdateName(
                        chatRoom,
                        "새로운 이름"
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void validateUpdateName_기존_이름과_동일하면_INVALID_INPUT_예외가_발생한다() {
        // given
        ChatRoom chatRoom = ChatRoom.group("스터디방");

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateUpdateName(
                        chatRoom,
                        "스터디방"
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_INPUT);
    }

    @Test
    void validateUpdateName_앞뒤_공백을_제거하면_기존_이름과_동일한_경우_INVALID_INPUT_예외가_발생한다() {
        // given
        ChatRoom chatRoom = ChatRoom.group("스터디방");

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateUpdateName(
                        chatRoom,
                        "  스터디방  "
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_INPUT);
    }

    @Test
    void validateMember_채팅방의_활성_멤버이면_사용자를_반환한다() {
        // given
        User user = mock(User.class);

        when(chatRoomMemberRepository.findUserInRoom(
                ROOM_ID,
                USER_ID
        )).thenReturn(Optional.of(user));

        // when
        User result =
                chatRoomValidator.validateMember(ROOM_ID, USER_ID);

        // then
        assertThat(result).isSameAs(user);

        verify(chatRoomMemberRepository)
                .findUserInRoom(ROOM_ID, USER_ID);
    }

    @Test
    void validateMember_채팅방의_멤버가_아니면_CHAT_ROOM_FORBIDDEN_예외가_발생한다() {
        // given
        when(chatRoomMemberRepository.findUserInRoom(
                ROOM_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                chatRoomValidator.validateMember(ROOM_ID, USER_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN);
    }
}