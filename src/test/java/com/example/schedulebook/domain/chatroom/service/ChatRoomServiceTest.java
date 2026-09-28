package com.example.schedulebook.domain.chatroom.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatroom.dto.request.ChatRoomInviteRequest;
import com.example.schedulebook.domain.chatroom.dto.request.GroupChatRoomCreateRequest;
import com.example.schedulebook.domain.chatroom.dto.request.ChatRoomUpdateNameRequest;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomDetailResponse;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomResponse;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomSliceResponse;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.entity.DirectChatRoom;
import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;
import com.example.schedulebook.domain.chatroom.projection.ChatRoomListProjection;
import com.example.schedulebook.domain.chatroom.projection.OpponentInfoProjection;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomRepository;
import com.example.schedulebook.domain.chatroom.repository.DirectChatRoomRepository;
import com.example.schedulebook.domain.chatroom.validator.ChatRoomValidator;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatRoomServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FRIEND_ID = 2L;
    private static final Long ANOTHER_FRIEND_ID = 3L;

    private static final Long ROOM_ID = 10L;
    private static final Long MEMBER_ID = 100L;

    private ChatRoomRepository chatRoomRepository;
    private ChatRoomMemberRepository chatRoomMemberRepository;
    private DirectChatRoomRepository directChatRoomRepository;
    private ChatRoomLifecycleManager chatRoomLifecycleManager;
    private UserValidator userValidator;
    private ChatRoomValidator chatRoomValidator;

    private ChatRoomService chatRoomService;

    private User currentUser;
    private User friendUser;
    private User anotherFriendUser;

    @BeforeEach
    void setUp() {
        chatRoomRepository = mock(ChatRoomRepository.class);
        chatRoomMemberRepository = mock(ChatRoomMemberRepository.class);
        directChatRoomRepository = mock(DirectChatRoomRepository.class);
        chatRoomLifecycleManager = mock(ChatRoomLifecycleManager.class);
        userValidator = mock(UserValidator.class);
        chatRoomValidator = mock(ChatRoomValidator.class);

        chatRoomService = new ChatRoomService(
                chatRoomRepository,
                chatRoomMemberRepository,
                directChatRoomRepository,
                chatRoomLifecycleManager,
                userValidator,
                chatRoomValidator
        );

        currentUser = mock(User.class);
        friendUser = mock(User.class);
        anotherFriendUser = mock(User.class);

        when(currentUser.getId()).thenReturn(USER_ID);
        when(currentUser.getNickname()).thenReturn("현재사용자");

        when(friendUser.getId()).thenReturn(FRIEND_ID);
        when(friendUser.getNickname()).thenReturn("친구");

        when(anotherFriendUser.getId()).thenReturn(ANOTHER_FRIEND_ID);
        when(anotherFriendUser.getNickname()).thenReturn("또다른친구");
    }

    @Test
    void createDirectRoom_자기_자신을_대상으로_지정하면_CANNOT_ADD_MYSELF_예외가_발생한다() {
        // given
        doThrow(new BaseException(ErrorEnum.CANNOT_ADD_MYSELF))
                .when(chatRoomValidator)
                .validateMyself(USER_ID, USER_ID);

        // when & then
        assertThatThrownBy(() ->
                chatRoomService.createDirectRoom(USER_ID, USER_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.CANNOT_ADD_MYSELF);

        verifyNoInteractions(userValidator);
        verifyNoInteractions(directChatRoomRepository);
        verify(chatRoomRepository, never())
                .save(any(ChatRoom.class));
    }

    @Test
    void createDirectRoom_친구가_아닌_사용자를_대상으로_지정하면_FRIEND_NOT_FOUND_예외가_발생한다() {
        // given
        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        doThrow(new BaseException(ErrorEnum.FRIEND_NOT_FOUND))
                .when(chatRoomValidator)
                .validateFriend(USER_ID, FRIEND_ID);

        // when & then
        assertThatThrownBy(() ->
                chatRoomService.createDirectRoom(USER_ID, FRIEND_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.FRIEND_NOT_FOUND);

        verify(directChatRoomRepository, never())
                .findByUser1IdAndUser2Id(anyLong(), anyLong());
    }

    @Test
    void createDirectRoom_기존_1대1_채팅방이_존재하면_새로_생성하지_않고_재사용한다() {
        // given
        ChatRoom chatRoom = createDirectChatRoom();

        DirectChatRoom directChatRoom = mock(DirectChatRoom.class);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(directChatRoomRepository.findByUser1IdAndUser2Id(
                USER_ID,
                FRIEND_ID
        )).thenReturn(Optional.of(directChatRoom));

        when(directChatRoom.getChatRoom())
                .thenReturn(chatRoom);

        // when
        ChatRoomResponse response =
                chatRoomService.createDirectRoom(USER_ID, FRIEND_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(ROOM_ID);
        assertThat(response.chatRoomType())
                .isEqualTo(ChatRoomType.DIRECT);

        verify(chatRoomValidator)
                .validateMyself(USER_ID, FRIEND_ID);

        verify(chatRoomValidator)
                .validateFriend(USER_ID, FRIEND_ID);

        verify(directChatRoomRepository)
                .findByUser1IdAndUser2Id(USER_ID, FRIEND_ID);

        verify(chatRoomRepository, never())
                .save(any(ChatRoom.class));

        verify(chatRoomMemberRepository, never())
                .saveAll(any());
    }

    @Test
    void createDirectRoom_사용자_ID_순서가_반대여도_정렬된_ID로_기존_채팅방을_조회한다() {
        // given
        ChatRoom chatRoom = createDirectChatRoom();

        DirectChatRoom directChatRoom = mock(DirectChatRoom.class);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        when(directChatRoomRepository.findByUser1IdAndUser2Id(
                USER_ID,
                FRIEND_ID
        )).thenReturn(Optional.of(directChatRoom));

        when(directChatRoom.getChatRoom())
                .thenReturn(chatRoom);

        // when
        ChatRoomResponse response =
                chatRoomService.createDirectRoom(FRIEND_ID, USER_ID);

        // then
        assertThat(response).isNotNull();

        verify(directChatRoomRepository)
                .findByUser1IdAndUser2Id(USER_ID, FRIEND_ID);
    }

    @Test
    void createDirectRoom_새로운_1대1_채팅방을_생성하고_두_명의_멤버를_등록한다() {
        // given
        ChatRoom updatedChatRoom = createDirectChatRoom();

        doAnswer(invocation -> {
            ChatRoom saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", ROOM_ID);
            return saved;
        }).when(chatRoomRepository)
                .save(any(ChatRoom.class));

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(directChatRoomRepository.findByUser1IdAndUser2Id(
                USER_ID,
                FRIEND_ID
        )).thenReturn(Optional.empty());

        when(chatRoomRepository.increaseMemberCount(ROOM_ID, 2))
                .thenReturn(1);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(updatedChatRoom);

        // when
        ChatRoomResponse response =
                chatRoomService.createDirectRoom(USER_ID, FRIEND_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(ROOM_ID);
        assertThat(response.chatRoomType())
                .isEqualTo(ChatRoomType.DIRECT);

        ArgumentCaptor<Iterable<ChatRoomMember>> memberCaptor =
                ArgumentCaptor.forClass(Iterable.class);

        verify(chatRoomMemberRepository)
                .saveAll(memberCaptor.capture());

        Iterable<ChatRoomMember> savedMembers =
                memberCaptor.getValue();

        int memberCount = 0;
        for (ChatRoomMember ignored : savedMembers) {
            memberCount++;
        }

        assertThat(memberCount).isEqualTo(2);

        verify(chatRoomRepository)
                .save(any(ChatRoom.class));

        verify(chatRoomRepository)
                .increaseMemberCount(ROOM_ID, 2);

        verify(directChatRoomRepository)
                .save(any(DirectChatRoom.class));
    }

    @Test
    void createDirectRoom_중복_생성_충돌이_발생하면_이미_생성된_채팅방을_재사용한다() {
        // given
        ChatRoom chatRoom = createDirectChatRoom();

        DirectChatRoom directChatRoom = mock(DirectChatRoom.class);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(directChatRoomRepository.findByUser1IdAndUser2Id(
                USER_ID,
                FRIEND_ID
        ))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(directChatRoom));

        when(directChatRoom.getChatRoom())
                .thenReturn(chatRoom);

        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(chatRoomRepository)
                .save(any(ChatRoom.class));

        // when
        ChatRoomResponse response =
                chatRoomService.createDirectRoom(USER_ID, FRIEND_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(ROOM_ID);

        verify(directChatRoomRepository, times(2))
                .findByUser1IdAndUser2Id(USER_ID, FRIEND_ID);
    }

    @Test
    void createGroupRoom_소유자와_초대_멤버를_등록하고_생성_후처리를_수행한다() {
        // given
        GroupChatRoomCreateRequest request =
                new GroupChatRoomCreateRequest(
                        "스터디방",
                        List.of(FRIEND_ID)
                );

        ChatRoom updatedChatRoom = createGroupChatRoom();
        ReflectionTestUtils.setField(
                updatedChatRoom,
                "memberCount",
                2
        );

        doAnswer(invocation -> {
            ChatRoom saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", ROOM_ID);
            return saved;
        }).when(chatRoomRepository)
                .save(any(ChatRoom.class));

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        when(chatRoomValidator.validateInviteMembers(
                USER_ID,
                request.memberIds()
        )).thenReturn(List.of(friendUser));

        when(chatRoomRepository.increaseMemberCount(ROOM_ID, 2))
                .thenReturn(1);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(updatedChatRoom);

        // when
        ChatRoomResponse response =
                chatRoomService.createGroupRoom(USER_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(ROOM_ID);
        assertThat(response.chatRoomType())
                .isEqualTo(ChatRoomType.GROUP);
        assertThat(response.roomName())
                .isEqualTo("스터디방");

        ArgumentCaptor<Iterable<ChatRoomMember>> memberCaptor =
                ArgumentCaptor.forClass(Iterable.class);

        verify(chatRoomMemberRepository)
                .saveAll(memberCaptor.capture());

        Iterable<ChatRoomMember> savedMembers =
                memberCaptor.getValue();

        int memberCount = 0;
        for (ChatRoomMember ignored : savedMembers) {
            memberCount++;
        }

        assertThat(memberCount).isEqualTo(2);

        verify(chatRoomRepository)
                .increaseMemberCount(ROOM_ID, 2);

        verify(chatRoomLifecycleManager)
                .afterRoomCreated(
                        updatedChatRoom,
                        currentUser
                );
    }

    @Test
    void inviteMembers_새로운_멤버를_초대하면_멤버를_추가한다() {
        // given
        ChatRoom chatRoom = createGroupChatRoom();
        ChatRoom updatedChatRoom = createGroupChatRoom();

        ChatRoomInviteRequest request =
                new ChatRoomInviteRequest(
                        List.of(FRIEND_ID)
                );

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom, updatedChatRoom);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(mock(ChatRoomMember.class));

        when(chatRoomValidator.validateInviteMembers(
                USER_ID,
                request.memberIds()
        )).thenReturn(List.of(friendUser));

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(
                ROOM_ID,
                FRIEND_ID
        )).thenReturn(Optional.empty());

        when(chatRoomRepository.increaseMemberCount(
                ROOM_ID,
                1
        )).thenReturn(1);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        // when
        ChatRoomResponse response =
                chatRoomService.inviteMembers(
                        USER_ID,
                        ROOM_ID,
                        request
                );

        // then
        assertThat(response).isNotNull();

        verify(chatRoomMemberRepository)
                .save(any(ChatRoomMember.class));

        verify(chatRoomRepository)
                .increaseMemberCount(ROOM_ID, 1);

        verify(chatRoomLifecycleManager)
                .afterMemberInvited(
                        updatedChatRoom,
                        currentUser,
                        List.of(friendUser)
                );
    }

    @Test
    void inviteMembers_이미_가입된_멤버를_초대하면_중복_추가하지_않는다() {
        // given
        ChatRoom chatRoom = createGroupChatRoom();
        ChatRoom updatedChatRoom = createGroupChatRoom();

        ChatRoomInviteRequest request =
                new ChatRoomInviteRequest(
                        List.of(FRIEND_ID)
                );

        ChatRoomMember activeMember =
                createChatRoomMember(chatRoom, friendUser);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom, updatedChatRoom);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(mock(ChatRoomMember.class));

        when(chatRoomValidator.validateInviteMembers(
                USER_ID,
                request.memberIds()
        )).thenReturn(List.of(friendUser));

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(
                ROOM_ID,
                FRIEND_ID
        )).thenReturn(Optional.of(activeMember));

        // when
        ChatRoomResponse response =
                chatRoomService.inviteMembers(
                        USER_ID,
                        ROOM_ID,
                        request
                );

        // then
        assertThat(response).isNotNull();

        verify(chatRoomMemberRepository, never())
                .save(any(ChatRoomMember.class));

        verify(chatRoomMemberRepository, never())
                .rejoin(anyLong(), any(LocalDateTime.class));

        verify(chatRoomRepository, never())
                .increaseMemberCount(anyLong(), anyInt());

        verify(chatRoomLifecycleManager, never())
                .afterMemberInvited(
                        any(),
                        any(),
                        anyList()
                );
    }

    @Test
    void inviteMembers_탈퇴한_멤버를_다시_초대하면_재가입시키고_인원수를_증가시킨다() {
        // given
        ChatRoom chatRoom = createGroupChatRoom();
        ChatRoom updatedChatRoom = createGroupChatRoom();

        ChatRoomInviteRequest request =
                new ChatRoomInviteRequest(
                        List.of(FRIEND_ID)
                );

        ChatRoomMember deletedMember =
                createChatRoomMember(chatRoom, friendUser);

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom, updatedChatRoom);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(mock(ChatRoomMember.class));

        when(chatRoomValidator.validateInviteMembers(
                USER_ID,
                request.memberIds()
        )).thenReturn(List.of(friendUser));

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(
                ROOM_ID,
                FRIEND_ID
        )).thenReturn(Optional.of(deletedMember));

        when(deletedMember.getDeletedAt())
                .thenReturn(LocalDateTime.now());

        when(chatRoomMemberRepository.rejoin(
                eq(MEMBER_ID),
                any(LocalDateTime.class)
        )).thenReturn(1);

        when(chatRoomRepository.increaseMemberCount(
                ROOM_ID,
                1
        )).thenReturn(1);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        // when
        ChatRoomResponse response =
                chatRoomService.inviteMembers(
                        USER_ID,
                        ROOM_ID,
                        request
                );

        // then
        assertThat(response).isNotNull();

        verify(chatRoomMemberRepository)
                .rejoin(
                        eq(MEMBER_ID),
                        any(LocalDateTime.class)
                );

        verify(chatRoomRepository)
                .increaseMemberCount(ROOM_ID, 1);

        verify(chatRoomLifecycleManager)
                .afterMemberInvited(
                        updatedChatRoom,
                        currentUser,
                        List.of(friendUser)
                );
    }

    @Test
    void findMyChatRooms_첫_페이지를_조회하면_다음_페이지가_없다() {
        // given
        ChatRoomListProjection projection =
                mock(ChatRoomListProjection.class);

        when(chatRoomMemberRepository.findMyChatRooms(
                eq(USER_ID),
                isNull(),
                isNull(),
                any()
        )).thenReturn(List.of(projection));

        // when
        ChatRoomSliceResponse response =
                chatRoomService.findMyChatRooms(
                        USER_ID,
                        null,
                        null,
                        20
                );

        // then
        assertThat(response).isNotNull();
        assertThat(response.chatRoomListResponses())
                .hasSize(1);
        assertThat(response.hasNext())
                .isFalse();
        assertThat(response.nextCursor())
                .isNull();

        verify(chatRoomMemberRepository)
                .findMyChatRooms(
                        eq(USER_ID),
                        isNull(),
                        isNull(),
                        any()
                );
    }

    @Test
    void findMyChatRooms_다음_페이지가_존재하면_마지막_채팅방을_기준으로_cursor를_생성한다() {
        // given
        ChatRoomListProjection first =
                mock(ChatRoomListProjection.class);

        ChatRoomListProjection second =
                mock(ChatRoomListProjection.class);

        ChatRoomListProjection third =
                mock(ChatRoomListProjection.class);

        LocalDateTime firstTime =
                LocalDateTime.of(2026, 9, 3, 12, 0);

        LocalDateTime secondTime =
                LocalDateTime.of(2026, 9, 2, 12, 0);

        LocalDateTime thirdTime =
                LocalDateTime.of(2026, 9, 1, 12, 0);

        when(first.getRoomId()).thenReturn(30L);
        when(first.getLastMessageAt()).thenReturn(firstTime);

        when(second.getRoomId()).thenReturn(20L);
        when(second.getLastMessageAt()).thenReturn(secondTime);

        when(third.getRoomId()).thenReturn(10L);
        when(third.getLastMessageAt()).thenReturn(thirdTime);

        when(chatRoomMemberRepository.findMyChatRooms(
                eq(USER_ID),
                isNull(),
                isNull(),
                any()
        )).thenReturn(
                List.of(first, second, third)
        );

        // when
        ChatRoomSliceResponse response =
                chatRoomService.findMyChatRooms(
                        USER_ID,
                        null,
                        null,
                        2
                );

        // then
        assertThat(response.hasNext())
                .isTrue();

        assertThat(response.chatRoomListResponses())
                .hasSize(2);

        assertThat(response.nextCursor())
                .isNotNull();

        assertThat(response.nextCursor().lastMessageAt())
                .isEqualTo(secondTime);

        assertThat(response.nextCursor().roomId())
                .isEqualTo(20L);
    }

    @Test
    void findMyChatRooms_cursorTime만_전달하면_INVALID_CURSOR_예외가_발생한다() {
        // given
        LocalDateTime cursorTime =
                LocalDateTime.of(2026, 9, 1, 12, 0);

        // when & then
        assertThatThrownBy(() ->
                chatRoomService.findMyChatRooms(
                        USER_ID,
                        cursorTime,
                        null,
                        20
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.INVALID_CURSOR);

        verifyNoInteractions(chatRoomMemberRepository);
    }

    @Test
    void findMyChatRooms_cursorTime과_cursorRoomId를_모두_전달하면_정상적으로_조회한다() {
        // given
        LocalDateTime cursorTime =
                LocalDateTime.of(2026, 9, 1, 12, 0);

        ChatRoomListProjection projection =
                mock(ChatRoomListProjection.class);

        when(chatRoomMemberRepository.findMyChatRooms(
                eq(USER_ID),
                eq(cursorTime),
                eq(ROOM_ID),
                any()
        )).thenReturn(List.of(projection));

        // when
        ChatRoomSliceResponse response =
                chatRoomService.findMyChatRooms(
                        USER_ID,
                        cursorTime,
                        ROOM_ID,
                        20
                );

        // then
        assertThat(response.hasNext())
                .isFalse();

        assertThat(response.nextCursor())
                .isNull();

        verify(chatRoomMemberRepository)
                .findMyChatRooms(
                        eq(USER_ID),
                        eq(cursorTime),
                        eq(ROOM_ID),
                        any()
                );
    }

    @Test
    void findChatRoom_멤버가_아니면_CHAT_ROOM_FORBIDDEN_예외가_발생한다() {
        // given
        doThrow(new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN))
                .when(chatRoomValidator)
                .validateChatRoomMember(USER_ID, ROOM_ID);

        // when & then
        assertThatThrownBy(() ->
                chatRoomService.findChatRoom(USER_ID, ROOM_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.CHAT_ROOM_FORBIDDEN);

        verify(chatRoomMemberRepository, never())
                .findReadStatuses(anyLong());
    }

    @Test
    void findChatRoom_1대1_채팅방에서_상대방_정보가_없으면_알수없음_닉네임을_사용한다() {
        // given
        ChatRoom chatRoom = createDirectChatRoom();

        ChatRoomMember member =
                createChatRoomMember(chatRoom, currentUser);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(member);

        when(chatRoomMemberRepository.findOpponentInfo(
                ROOM_ID,
                USER_ID
        )).thenReturn(null);

        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        ChatRoomDetailResponse response =
                chatRoomService.findChatRoom(USER_ID, ROOM_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomName())
                .isEqualTo("알 수 없음");

        verify(chatRoomMemberRepository)
                .findOpponentInfo(ROOM_ID, USER_ID);
    }


    @Test
    void findChatRoom_1대1_채팅방에서_삭제된_상대방이면_알수없음_닉네임을_사용한다() {
        // given
        ChatRoom chatRoom = createDirectChatRoom();

        ChatRoomMember member =
                createChatRoomMember(chatRoom, currentUser);

        OpponentInfoProjection opponent =
                mock(OpponentInfoProjection.class);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(member);

        when(chatRoomMemberRepository.findOpponentInfo(
                ROOM_ID,
                USER_ID
        )).thenReturn(opponent);

        when(opponent.getUserDeletedAt())
                .thenReturn(LocalDateTime.now());

        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        ChatRoomDetailResponse response =
                chatRoomService.findChatRoom(USER_ID, ROOM_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomName())
                .isEqualTo("알 수 없음");

        verify(opponent)
                .getUserDeletedAt();
    }

    @Test
    void findChatRoom_그룹_채팅방이면_채팅방_이름을_사용한다() {
        // given
        ChatRoom chatRoom = createGroupChatRoom();

        ChatRoomMember member =
                createChatRoomMember(chatRoom, currentUser);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(member);

        when(chatRoomMemberRepository.findReadStatuses(ROOM_ID))
                .thenReturn(List.of());

        // when
        ChatRoomDetailResponse response =
                chatRoomService.findChatRoom(USER_ID, ROOM_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomName())
                .isEqualTo("스터디방");

        verify(chatRoomMemberRepository, never())
                .findOpponentInfo(anyLong(), anyLong());
    }

    @Test
    void updateRoomName_채팅방_이름을_수정하면_trim된_이름으로_변경하고_후처리를_수행한다() {
        // given
        ChatRoom chatRoom = createGroupChatRoom();

        ChatRoomUpdateNameRequest request =
                new ChatRoomUpdateNameRequest("  새로운 이름  ");

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(chatRoom);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        // when
        ChatRoomResponse response =
                chatRoomService.updateRoomName(
                        USER_ID,
                        ROOM_ID,
                        request
                );

        // then
        assertThat(response).isNotNull();

        assertThat(chatRoom.getName())
                .isEqualTo("새로운 이름");

        verify(chatRoomValidator)
                .validateUpdateName(
                        chatRoom,
                        "새로운 이름"
                );

        verify(chatRoomLifecycleManager)
                .afterRoomNameUpdated(
                        chatRoom,
                        currentUser,
                        "스터디방",
                        "새로운 이름"
                );
    }

    @Test
    void leaveChatRoom_1대1_채팅방이면_멤버를_탈퇴처리한다() {
        // given
        ChatRoom chatRoom = createDirectChatRoom();

        ChatRoomMember member =
                createChatRoomMember(chatRoom, currentUser);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(member);

        // when
        chatRoomService.leaveChatRoom(USER_ID, ROOM_ID);

        // then
        verify(chatRoomMemberRepository)
                .leave(
                        eq(MEMBER_ID),
                        any(LocalDateTime.class)
                );

        verify(chatRoomRepository, never())
                .decreaseMemberCount(anyLong());

        verify(chatRoomLifecycleManager, never())
                .afterMemberLeft(any(), anyString());
    }

    @Test
    void leaveChatRoom_그룹_채팅방이면_정상적으로_탈퇴처리하고_인원수를_감소시킨다() {
        // given
        ChatRoom chatRoom = createGroupChatRoom();

        ChatRoomMember member =
                createChatRoomMember(chatRoom, currentUser);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(member);

        when(chatRoomMemberRepository.leave(
                eq(MEMBER_ID),
                any(LocalDateTime.class)
        )).thenReturn(1);

        when(chatRoomRepository.decreaseMemberCount(ROOM_ID))
                .thenReturn(1);

        ChatRoom updatedChatRoom = createGroupChatRoom();

        when(chatRoomValidator.validateChatRoom(ROOM_ID))
                .thenReturn(updatedChatRoom);

        // when
        chatRoomService.leaveChatRoom(USER_ID, ROOM_ID);

        // then
        verify(chatRoomRepository)
                .decreaseMemberCount(ROOM_ID);

        verify(chatRoomValidator)
                .validateChatRoom(ROOM_ID);

        verify(chatRoomLifecycleManager)
                .afterMemberLeft(
                        updatedChatRoom,
                        "현재사용자"
                );
    }

    @Test
    void leaveChatRoom_그룹_채팅방_멤버_탈퇴_처리가_실패하면_인원수와_후처리를_수행하지_않는다() {
        // given
        ChatRoom chatRoom = createGroupChatRoom();

        ChatRoomMember member =
                createChatRoomMember(chatRoom, currentUser);

        when(chatRoomValidator.validateChatRoomMember(
                USER_ID,
                ROOM_ID
        )).thenReturn(member);

        when(chatRoomMemberRepository.leave(
                eq(MEMBER_ID),
                any(LocalDateTime.class)
        )).thenReturn(0);

        // when & then
        assertThatCode(() ->
                chatRoomService.leaveChatRoom(USER_ID, ROOM_ID)
        ).doesNotThrowAnyException();

        verify(chatRoomRepository, never())
                .decreaseMemberCount(anyLong());

        verify(chatRoomLifecycleManager, never())
                .afterMemberLeft(any(), anyString());
    }

    @Test
    void removeAllChatRelations_사용자_삭제시_모든_채팅방_관계를_정리한다() {
        // given
        ChatRoom groupRoom = createGroupChatRoom();

        ChatRoomMember member =
                createChatRoomMember(groupRoom, currentUser);

        when(chatRoomMemberRepository.findAllByUserId(USER_ID))
                .thenReturn(List.of(member));

        when(chatRoomMemberRepository.leave(
                eq(MEMBER_ID),
                any(LocalDateTime.class)
        )).thenReturn(1);

        when(chatRoomRepository.decreaseMemberCount(ROOM_ID))
                .thenReturn(1);

        // when
        chatRoomService.removeAllChatRelations(USER_ID);

        // then
        verify(chatRoomMemberRepository)
                .findAllByUserId(USER_ID);

        verify(chatRoomMemberRepository)
                .leave(
                        eq(MEMBER_ID),
                        any(LocalDateTime.class)
                );

        verify(chatRoomRepository)
                .decreaseMemberCount(ROOM_ID);

        verify(directChatRoomRepository)
                .deleteDirectChatRoomByUserId(USER_ID);
    }

    private ChatRoom createDirectChatRoom() {
        ChatRoom chatRoom = ChatRoom.direct();

        ReflectionTestUtils.setField(
                chatRoom,
                "id",
                ROOM_ID
        );

        return chatRoom;
    }

    private ChatRoom createGroupChatRoom() {
        ChatRoom chatRoom = ChatRoom.group("스터디방");

        ReflectionTestUtils.setField(
                chatRoom,
                "id",
                ROOM_ID
        );

        return chatRoom;
    }

    private ChatRoomMember createChatRoomMember(
            ChatRoom chatRoom,
            User user
    ) {
        ChatRoomMember member = mock(ChatRoomMember.class);

        when(member.getId()).thenReturn(MEMBER_ID);
        when(member.getChatRoom()).thenReturn(chatRoom);
        when(member.getUser()).thenReturn(user);

        return member;
    }
}