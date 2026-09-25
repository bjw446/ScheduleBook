package com.example.schedulebook.domain.friend.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendValidatorTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private Friend friend;

    @Mock
    private User requester;

    @Mock
    private User receiver;

    private FriendValidator friendValidator;

    private static final Long FRIEND_ID = 1L;
    private static final Long REQUESTER_ID = 2L;
    private static final Long RECEIVER_ID = 3L;
    private static final Long OTHER_USER_ID = 4L;

    @BeforeEach
    void setUp() {
        friendValidator = new FriendValidator(friendRepository);
    }

    @Test
    void 존재하는_친구_관계를_조회하면_friend를_반환한다() {
        // given
        when(friendRepository.findByIdWithUsers(FRIEND_ID))
                .thenReturn(Optional.of(friend));

        // when
        Friend result = friendValidator.validateFriend(FRIEND_ID);

        // then
        assertThat(result).isSameAs(friend);

        verify(friendRepository).findByIdWithUsers(FRIEND_ID);
    }

    @Test
    void 존재하지_않는_친구_관계를_조회하면_friend_not_found_예외가_발생한다() {
        // given
        when(friendRepository.findByIdWithUsers(FRIEND_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                friendValidator.validateFriend(FRIEND_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.FRIEND_NOT_FOUND);

        verify(friendRepository).findByIdWithUsers(FRIEND_ID);
    }

    @Test
    void receiver가_현재_사용자라면_검증을_통과한다() {
        // given
        when(friend.getReceiver()).thenReturn(receiver);
        when(receiver.getId()).thenReturn(RECEIVER_ID);

        // when
        friendValidator.validateReceiver(friend, RECEIVER_ID);

        // then
        verify(friend).getReceiver();
        verify(receiver).getId();
    }

    @Test
    void receiver가_현재_사용자가_아니면_friend_forbidden_예외가_발생한다() {
        // given
        when(friend.getReceiver()).thenReturn(receiver);
        when(receiver.getId()).thenReturn(RECEIVER_ID);

        // when & then
        assertThatThrownBy(() ->
                friendValidator.validateReceiver(friend, OTHER_USER_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.FRIEND_FORBIDDEN);
    }

    @Test
    void 친구_상태가_PENDING이면_검증을_통과한다() {
        // given
        when(friend.getFriendStatus()).thenReturn(FriendStatus.PENDING);

        // when
        friendValidator.validatePending(friend);

        // then
        verify(friend).getFriendStatus();
    }

    @Test
    void 친구_상태가_PENDING이_아니면_invalid_friend_status_예외가_발생한다() {
        // given
        when(friend.getFriendStatus()).thenReturn(FriendStatus.ACCEPTED);

        // when & then
        assertThatThrownBy(() ->
                friendValidator.validatePending(friend)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_FRIEND_STATUS);
    }

    @Test
    void 자기_자신에게_친구_요청하면_cannot_add_myself_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                friendValidator.validateMyself(REQUESTER_ID, REQUESTER_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.CANNOT_ADD_MYSELF);
    }

    @Test
    void 다른_사용자에게_친구_요청하면_검증을_통과한다() {
        // when
        friendValidator.validateMyself(RECEIVER_ID, REQUESTER_ID);

        // then
        // 예외가 발생하지 않으면 검증 성공
    }

    @Test
    void requester가_현재_사용자라면_친구_소유자_검증을_통과한다() {
        // given
        when(friend.getRequester()).thenReturn(requester);
        when(requester.getId()).thenReturn(REQUESTER_ID);

        // when
        friendValidator.validateFriendOwner(friend, REQUESTER_ID);

        // then
        verify(friend).getRequester();
        verify(requester).getId();
        verify(friend, never()).getReceiver();
    }

    @Test
    void receiver가_현재_사용자라면_친구_소유자_검증을_통과한다() {
        // given
        when(friend.getRequester()).thenReturn(requester);
        when(requester.getId()).thenReturn(REQUESTER_ID);

        when(friend.getReceiver()).thenReturn(receiver);
        when(receiver.getId()).thenReturn(RECEIVER_ID);

        // when
        friendValidator.validateFriendOwner(friend, RECEIVER_ID);

        // then
        verify(friend).getRequester();
        verify(requester).getId();
        verify(friend).getReceiver();
        verify(receiver).getId();
    }

    @Test
    void requester와_receiver가_모두_현재_사용자가_아니면_friend_forbidden_예외가_발생한다() {
        // given
        when(friend.getRequester()).thenReturn(requester);
        when(requester.getId()).thenReturn(REQUESTER_ID);

        when(friend.getReceiver()).thenReturn(receiver);
        when(receiver.getId()).thenReturn(RECEIVER_ID);

        // when & then
        assertThatThrownBy(() ->
                friendValidator.validateFriendOwner(friend, OTHER_USER_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.FRIEND_FORBIDDEN);
    }

    @Test
    void 친구_상태가_ACCEPTED이면_삭제_가능_검증을_통과한다() {
        // given
        when(friend.getFriendStatus()).thenReturn(FriendStatus.ACCEPTED);

        // when
        friendValidator.validateDeletable(friend);

        // then
        verify(friend).getFriendStatus();
    }

    @Test
    void 친구_상태가_ACCEPTED가_아니면_invalid_friend_status_예외가_발생한다() {
        // given
        when(friend.getFriendStatus()).thenReturn(FriendStatus.PENDING);

        // when & then
        assertThatThrownBy(() ->
                friendValidator.validateDeletable(friend)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_FRIEND_STATUS);
    }

    @Test
    void 현재_사용자가_requester라면_상대방인_receiver를_반환한다() {
        // given
        when(friend.getRequester()).thenReturn(requester);
        when(requester.getId()).thenReturn(REQUESTER_ID);
        when(friend.getReceiver()).thenReturn(receiver);

        // when
        User result = friendValidator.extractFriendUser(
                friend,
                REQUESTER_ID
        );

        // then
        assertThat(result).isSameAs(receiver);

        verify(requester).getId();
        verify(friend).getReceiver();
    }

    @Test
    void 현재_사용자가_requester가_아니라면_requester를_상대방으로_반환한다() {
        // given
        when(friend.getRequester()).thenReturn(requester);
        when(requester.getId()).thenReturn(REQUESTER_ID);

        // when
        User result = friendValidator.extractFriendUser(
                friend,
                RECEIVER_ID
        );

        // then
        assertThat(result).isSameAs(requester);

        verify(requester).getId();
        verify(friend, never()).getReceiver();
    }

    @Test
    void 친구_상태가_REJECTED이면_재요청_가능한_상태로_검증을_통과한다() {
        // given
        when(friend.getFriendStatus()).thenReturn(FriendStatus.REJECTED);

        // when & then
        assertThatCode(() ->
                friendValidator.validateFriendStatus(friend)
        )
                .doesNotThrowAnyException();
    }

    @Test
    void 친구_상태가_DELETED이면_재요청_가능한_상태로_검증을_통과한다() {
        // given
        when(friend.getFriendStatus()).thenReturn(FriendStatus.DELETED);

        // when & then
        assertThatCode(() ->
                friendValidator.validateFriendStatus(friend)
        )
                .doesNotThrowAnyException();
    }

    @Test
    void 친구_상태가_REJECTED나_DELETED가_아니면_friend_already_exists_예외가_발생한다() {
        // given
        when(friend.getFriendStatus()).thenReturn(FriendStatus.ACCEPTED);

        // when & then
        assertThatThrownBy(() ->
                friendValidator.validateFriendStatus(friend)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.FRIEND_ALREADY_EXISTS);
    }

    @Test
    void ACCEPTED_친구_관계가_존재하면_검증을_통과한다() {
        // given
        when(friendRepository.existsAcceptedFriend(
                REQUESTER_ID,
                RECEIVER_ID,
                FriendStatus.ACCEPTED
        )).thenReturn(true);

        // when
        friendValidator.validateFriendRelation(
                REQUESTER_ID,
                RECEIVER_ID
        );

        // then
        verify(friendRepository).existsAcceptedFriend(
                REQUESTER_ID,
                RECEIVER_ID,
                FriendStatus.ACCEPTED
        );
    }

    @Test
    void ACCEPTED_친구_관계가_없으면_friend_not_found_예외가_발생한다() {
        // given
        when(friendRepository.existsAcceptedFriend(
                REQUESTER_ID,
                RECEIVER_ID,
                FriendStatus.ACCEPTED
        )).thenReturn(false);

        // when & then
        assertThatThrownBy(() ->
                friendValidator.validateFriendRelation(
                        REQUESTER_ID,
                        RECEIVER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.FRIEND_NOT_FOUND);

        verify(friendRepository).existsAcceptedFriend(
                REQUESTER_ID,
                RECEIVER_ID,
                FriendStatus.ACCEPTED
        );
    }

    @Test
    void 자기_자신의_presence는_친구_관계_조회없이_접근할_수_있다() {
        // when
        friendValidator.validatePresenceAccess(
                REQUESTER_ID,
                REQUESTER_ID
        );

        // then
        verifyNoInteractions(friendRepository);
    }

    @Test
    void ACCEPTED_친구라면_presence_조회에_접근할_수_있다() {
        // given
        when(friendRepository.existsAcceptedFriend(
                REQUESTER_ID,
                RECEIVER_ID,
                FriendStatus.ACCEPTED
        )).thenReturn(true);

        // when
        friendValidator.validatePresenceAccess(
                REQUESTER_ID,
                RECEIVER_ID
        );

        // then
        verify(friendRepository).existsAcceptedFriend(
                REQUESTER_ID,
                RECEIVER_ID,
                FriendStatus.ACCEPTED
        );
    }

    @Test
    void 친구가_아니면_presence_access_denied_예외가_발생한다() {
        // given
        when(friendRepository.existsAcceptedFriend(
                REQUESTER_ID,
                OTHER_USER_ID,
                FriendStatus.ACCEPTED
        )).thenReturn(false);

        // when & then
        assertThatThrownBy(() ->
                friendValidator.validatePresenceAccess(
                        REQUESTER_ID,
                        OTHER_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.PRESENCE_ACCESS_DENIED);

        verify(friendRepository).existsAcceptedFriend(
                REQUESTER_ID,
                OTHER_USER_ID,
                FriendStatus.ACCEPTED
        );
    }
}