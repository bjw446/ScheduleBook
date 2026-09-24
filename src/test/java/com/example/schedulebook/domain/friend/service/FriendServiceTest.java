package com.example.schedulebook.domain.friend.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.friend.dto.request.FriendRequest;
import com.example.schedulebook.domain.friend.dto.response.FriendResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendSummaryResponse;
import com.example.schedulebook.domain.friend.dto.response.ReceivedFriendRequestResponse;
import com.example.schedulebook.domain.friend.dto.response.SentFriendRequestResponse;
import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.event.FriendAcceptedEvent;
import com.example.schedulebook.domain.friend.event.FriendRequestedEvent;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.friend.validator.FriendValidator;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private FriendValidator friendValidator;

    @Mock
    private RedisPresenceService redisPresenceService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private FriendRequest friendRequest;

    @Mock
    private User requester;

    @Mock
    private User receiver;

    @Mock
    private Friend friend;

    @Mock
    private FriendResponse friendResponse;

    private FriendService friendService;

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long FRIEND_ID = 10L;

    @BeforeEach
    void setUp() {
        friendService = new FriendService(
                friendRepository,
                userValidator,
                friendValidator,
                redisPresenceService,
                outboxService
        );
    }

    @Test
    void 친구_요청이_정상이면_친구관계를_저장하고_requested_event를_outbox에_저장한다() {
        // given
        when(friendRequest.receiverId()).thenReturn(RECEIVER_ID);

        when(requester.getId()).thenReturn(CURRENT_USER_ID);
        when(receiver.getId()).thenReturn(RECEIVER_ID);
        when(requester.getNickname()).thenReturn("requester");

        when(userValidator.validateActiveUser(CURRENT_USER_ID))
                .thenReturn(requester);
        when(userValidator.validateActiveUser(RECEIVER_ID))
                .thenReturn(receiver);

        when(friendRepository.findRelation(
                CURRENT_USER_ID,
                RECEIVER_ID
        )).thenReturn(Optional.empty());

        Friend savedFriend = mock(Friend.class);

        when(savedFriend.getId()).thenReturn(FRIEND_ID);

        try (MockedStatic<Friend> mockedFriend = mockStatic(Friend.class);
             MockedStatic<FriendResponse> mockedResponse = mockStatic(FriendResponse.class)) {

            mockedFriend.when(() ->
                    Friend.request(requester, receiver)
            ).thenReturn(friend);

            when(friendRepository.save(friend))
                    .thenReturn(savedFriend);

            mockedResponse.when(() ->
                    FriendResponse.from(savedFriend)
            ).thenReturn(friendResponse);

            // when
            FriendResponse result =
                    friendService.requestFriend(
                            friendRequest,
                            CURRENT_USER_ID
                    );

            // then
            assertThat(result).isSameAs(friendResponse);

            verify(friendValidator)
                    .validateMyself(RECEIVER_ID, CURRENT_USER_ID);

            verify(friendRepository)
                    .findRelation(
                            CURRENT_USER_ID,
                            RECEIVER_ID
                    );

            verify(friendRepository)
                    .save(friend);

            ArgumentCaptor<FriendRequestedEvent> eventCaptor =
                    ArgumentCaptor.forClass(FriendRequestedEvent.class);

            verify(outboxService).save(
                    anyString(),
                    eq(OutboxAggregateType.FRIEND),
                    eq(String.valueOf(FRIEND_ID)),
                    eq(OutboxEventType.FRIEND_REQUESTED),
                    eventCaptor.capture()
            );

            FriendRequestedEvent event = eventCaptor.getValue();

            assertThat(event.receiverId())
                    .isEqualTo(RECEIVER_ID);

            assertThat(event.requesterNickname())
                    .isEqualTo("requester");

            verify(savedFriend, times(2))
                    .getId();
        }
    }

    @Test
    void 자기_자신에게_친구_요청하면_이후_로직을_실행하지_않는다() {
        // given
        when(friendRequest.receiverId())
                .thenReturn(CURRENT_USER_ID);

        doThrow(new BaseException(ErrorEnum.CANNOT_ADD_MYSELF))
                .when(friendValidator)
                .validateMyself(
                        CURRENT_USER_ID,
                        CURRENT_USER_ID
                );

        // when & then
        assertThatThrownBy(() ->
                friendService.requestFriend(
                        friendRequest,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.CANNOT_ADD_MYSELF);

        verify(userValidator, never())
                .validateActiveUser(anyLong());

        verifyNoInteractions(friendRepository);
        verifyNoInteractions(outboxService);
    }

    @Test
    void 기존_관계가_있으면_새로운_친구관계를_생성하지_않고_재요청한다() {
        // given
        when(friendRequest.receiverId())
                .thenReturn(RECEIVER_ID);

        when(requester.getId())
                .thenReturn(CURRENT_USER_ID);

        when(receiver.getId())
                .thenReturn(RECEIVER_ID);

        when(requester.getNickname())
                .thenReturn("requester");

        when(friend.getId())
                .thenReturn(FRIEND_ID);

        when(userValidator.validateActiveUser(CURRENT_USER_ID))
                .thenReturn(requester);

        when(userValidator.validateActiveUser(RECEIVER_ID))
                .thenReturn(receiver);

        when(friendRepository.findRelation(
                CURRENT_USER_ID,
                RECEIVER_ID
        )).thenReturn(Optional.of(friend));

        try (MockedStatic<FriendResponse> mockedResponse =
                     mockStatic(FriendResponse.class)) {

            mockedResponse.when(() ->
                    FriendResponse.from(friend)
            ).thenReturn(friendResponse);

            // when
            FriendResponse result =
                    friendService.requestFriend(
                            friendRequest,
                            CURRENT_USER_ID
                    );

            // then
            assertThat(result)
                    .isSameAs(friendResponse);

            verify(friendValidator)
                    .validateFriendStatus(friend);

            verify(friend)
                    .reRequest(requester, receiver);

            verify(friendRepository, never())
                    .save(any(Friend.class));

            verify(outboxService).save(
                    anyString(),
                    eq(OutboxAggregateType.FRIEND),
                    eq(String.valueOf(FRIEND_ID)),
                    eq(OutboxEventType.FRIEND_REQUESTED),
                    any(FriendRequestedEvent.class)
            );
        }
    }

    @Test
    void 수신_사용자가_존재하지_않으면_친구관계를_생성하지_않는다() {
        // given
        when(friendRequest.receiverId())
                .thenReturn(RECEIVER_ID);

        when(userValidator.validateActiveUser(CURRENT_USER_ID))
                .thenReturn(requester);

        doThrow(new BaseException(ErrorEnum.USER_NOT_FOUND))
                .when(userValidator)
                .validateActiveUser(RECEIVER_ID);

        // when & then
        assertThatThrownBy(() ->
                friendService.requestFriend(
                        friendRequest,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.USER_NOT_FOUND);

        verify(friendRepository, never())
                .save(any(Friend.class));

        verifyNoInteractions(outboxService);
    }

    @Test
    void 친구관계_저장중_중복_무결성_예외가_발생하면_ALREADY_EXISTS로_변환한다() {
        // given
        when(friendRequest.receiverId())
                .thenReturn(RECEIVER_ID);

        when(requester.getId())
                .thenReturn(CURRENT_USER_ID);

        when(receiver.getId())
                .thenReturn(RECEIVER_ID);

        when(userValidator.validateActiveUser(CURRENT_USER_ID))
                .thenReturn(requester);

        when(userValidator.validateActiveUser(RECEIVER_ID))
                .thenReturn(receiver);

        when(friendRepository.findRelation(
                CURRENT_USER_ID,
                RECEIVER_ID
        )).thenReturn(Optional.empty());

        try (MockedStatic<Friend> mockedFriend =
                     mockStatic(Friend.class)) {

            mockedFriend.when(() ->
                    Friend.request(requester, receiver)
            ).thenReturn(friend);

            when(friendRepository.save(friend))
                    .thenThrow(
                            new DataIntegrityViolationException("duplicate")
                    );

            // when & then
            assertThatThrownBy(() ->
                    friendService.requestFriend(
                            friendRequest,
                            CURRENT_USER_ID
                    )
            )
                    .isInstanceOf(BaseException.class)
                    .extracting(exception ->
                            ((BaseException) exception).getErrorEnum()
                    )
                    .isEqualTo(ErrorEnum.FRIEND_ALREADY_EXISTS);

            verifyNoInteractions(outboxService);
        }
    }

    @Test
    void 친구_목록은_ACCEPTED_친구와_온라인_상태를_반환한다() {
        // given
        User friendUser = mock(User.class);

        when(friend.getId())
                .thenReturn(FRIEND_ID);

        when(friendUser.getId())
                .thenReturn(RECEIVER_ID);

        when(friendRepository.findAcceptedFriends(
                CURRENT_USER_ID,
                FriendStatus.ACCEPTED
        )).thenReturn(List.of(friend));

        when(friendValidator.extractFriendUser(
                friend,
                CURRENT_USER_ID
        )).thenReturn(friendUser);

        when(redisPresenceService.getOnlineStatuses(
                List.of(RECEIVER_ID)
        )).thenReturn(Map.of(
                RECEIVER_ID,
                true
        ));

        FriendSummaryResponse response =
                mock(FriendSummaryResponse.class);

        try (MockedStatic<FriendSummaryResponse> mockedResponse =
                     mockStatic(FriendSummaryResponse.class)) {

            mockedResponse.when(() ->
                    FriendSummaryResponse.from(
                            FRIEND_ID,
                            friendUser,
                            true
                    )
            ).thenReturn(response);

            // when
            List<FriendSummaryResponse> result =
                    friendService.findAllFriends(
                            CURRENT_USER_ID
                    );

            // then
            assertThat(result)
                    .containsExactly(response);

            verify(userValidator)
                    .validateActiveUser(CURRENT_USER_ID);

            verify(friendRepository)
                    .findAcceptedFriends(
                            CURRENT_USER_ID,
                            FriendStatus.ACCEPTED
                    );

            verify(friendValidator, times(2))
                    .extractFriendUser(
                            friend,
                            CURRENT_USER_ID
                    );

            verify(redisPresenceService)
                    .getOnlineStatuses(
                            List.of(RECEIVER_ID)
                    );
        }
    }

    @Test
    void 받은_친구_요청은_PENDING_상태로_조회한다() {
        // given
        when(friendRepository.findReceivedRequests(
                CURRENT_USER_ID,
                FriendStatus.PENDING
        )).thenReturn(List.of(friend));

        ReceivedFriendRequestResponse response =
                mock(ReceivedFriendRequestResponse.class);

        try (MockedStatic<ReceivedFriendRequestResponse> mockedResponse =
                     mockStatic(ReceivedFriendRequestResponse.class)) {

            mockedResponse.when(() ->
                    ReceivedFriendRequestResponse.from(friend)
            ).thenReturn(response);

            // when
            List<ReceivedFriendRequestResponse> result =
                    friendService.findReceivedRequests(
                            CURRENT_USER_ID
                    );

            // then
            assertThat(result)
                    .containsExactly(response);

            verify(userValidator)
                    .validateActiveUser(CURRENT_USER_ID);

            verify(friendRepository)
                    .findReceivedRequests(
                            CURRENT_USER_ID,
                            FriendStatus.PENDING
                    );
        }
    }

    @Test
    void 보낸_친구_요청은_PENDING_상태로_조회한다() {
        // given
        when(friendRepository.findSentRequests(
                CURRENT_USER_ID,
                FriendStatus.PENDING
        )).thenReturn(List.of(friend));

        SentFriendRequestResponse response =
                mock(SentFriendRequestResponse.class);

        try (MockedStatic<SentFriendRequestResponse> mockedResponse =
                     mockStatic(SentFriendRequestResponse.class)) {

            mockedResponse.when(() ->
                    SentFriendRequestResponse.from(friend)
            ).thenReturn(response);

            // when
            List<SentFriendRequestResponse> result =
                    friendService.findSentRequests(
                            CURRENT_USER_ID
                    );

            // then
            assertThat(result)
                    .containsExactly(response);

            verify(userValidator)
                    .validateActiveUser(CURRENT_USER_ID);

            verify(friendRepository)
                    .findSentRequests(
                            CURRENT_USER_ID,
                            FriendStatus.PENDING
                    );
        }
    }

    @Test
    void 친구_요청을_수락하면_ACCEPTED로_변경하고_accepted_event를_저장한다() {
        // given
        when(friend.getRequester())
                .thenReturn(requester);

        when(friend.getReceiver())
                .thenReturn(receiver);

        when(friend.getId())
                .thenReturn(FRIEND_ID);

        when(requester.getId())
                .thenReturn(CURRENT_USER_ID);

        when(receiver.getNickname())
                .thenReturn("receiver");

        when(friendValidator.validateFriend(FRIEND_ID))
                .thenReturn(friend);

        FriendResponse response =
                mock(FriendResponse.class);

        try (MockedStatic<FriendResponse> mockedResponse =
                     mockStatic(FriendResponse.class)) {

            mockedResponse.when(() ->
                    FriendResponse.from(friend)
            ).thenReturn(response);

            // when
            FriendResponse result =
                    friendService.acceptFriend(
                            FRIEND_ID,
                            CURRENT_USER_ID
                    );

            // then
            assertThat(result)
                    .isSameAs(response);

            verify(userValidator)
                    .validateActiveUser(CURRENT_USER_ID);

            verify(friendValidator)
                    .validateFriend(FRIEND_ID);

            verify(friendValidator)
                    .validateReceiver(
                            friend,
                            CURRENT_USER_ID
                    );

            verify(friendValidator)
                    .validatePending(friend);

            verify(friend)
                    .acceptFriend();

            verify(outboxService).save(
                    anyString(),
                    eq(OutboxAggregateType.FRIEND),
                    eq(String.valueOf(FRIEND_ID)),
                    eq(OutboxEventType.FRIEND_ACCEPTED),
                    any(FriendAcceptedEvent.class)
            );
        }
    }

    @Test
    void 친구_요청을_거절하면_REJECTED로_변경한다() {
        // given
        when(friendValidator.validateFriend(FRIEND_ID))
                .thenReturn(friend);

        // when
        friendService.rejectFriend(
                FRIEND_ID,
                CURRENT_USER_ID
        );

        // then
        verify(userValidator)
                .validateActiveUser(CURRENT_USER_ID);

        verify(friendValidator)
                .validateFriend(FRIEND_ID);

        verify(friendValidator)
                .validateReceiver(
                        friend,
                        CURRENT_USER_ID
                );

        verify(friendValidator)
                .validatePending(friend);

        verify(friend)
                .rejectFriend();

        verifyNoInteractions(outboxService);
    }

    @Test
    void 친구를_차단하면_BLOCKED로_변경한다() {
        // given
        when(friendValidator.validateFriend(FRIEND_ID))
                .thenReturn(friend);

        // when
        friendService.blockFriend(
                FRIEND_ID,
                CURRENT_USER_ID
        );

        // then
        verify(userValidator)
                .validateActiveUser(CURRENT_USER_ID);

        verify(friendValidator)
                .validateFriend(FRIEND_ID);

        verify(friendValidator)
                .validateFriendOwner(
                        friend,
                        CURRENT_USER_ID
                );

        verify(friend)
                .block();

        verifyNoInteractions(outboxService);
    }

    @Test
    void 친구를_삭제하면_DELETED로_변경한다() {
        // given
        when(friendValidator.validateFriend(FRIEND_ID))
                .thenReturn(friend);

        // when
        friendService.deleteFriend(
                FRIEND_ID,
                CURRENT_USER_ID
        );

        // then
        verify(userValidator)
                .validateActiveUser(CURRENT_USER_ID);

        verify(friendValidator)
                .validateFriend(FRIEND_ID);

        verify(friendValidator)
                .validateFriendOwner(
                        friend,
                        CURRENT_USER_ID
                );

        verify(friendValidator)
                .validateDeletable(friend);

        verify(friend)
                .deleteFriend();

        verifyNoInteractions(outboxService);
    }

    @Test
    void 사용자_탈퇴시_모든_친구관계를_삭제한다() {
        // when
        friendService.removeAllFriendRelations(
                CURRENT_USER_ID
        );

        // then
        verify(friendRepository)
                .deleteAllByUserId(CURRENT_USER_ID);
    }
}