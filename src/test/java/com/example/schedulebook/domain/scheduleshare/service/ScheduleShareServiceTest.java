package com.example.schedulebook.domain.scheduleshare.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.validator.FriendValidator;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.validator.ScheduleValidator;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleAttendanceResponse;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleparticipant.enums.AttendanceStatus;
import com.example.schedulebook.domain.scheduleparticipant.publisher.ScheduleAttendancePublisher;
import com.example.schedulebook.domain.scheduleparticipant.publisher.ScheduleParticipantPublisher;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.scheduleparticipant.service.ScheduleParticipantReader;
import com.example.schedulebook.domain.scheduleparticipant.validator.ScheduleParticipantValidator;
import com.example.schedulebook.domain.scheduleshare.dto.request.ScheduleShareRequest;
import com.example.schedulebook.domain.scheduleshare.dto.request.UpdateAttendanceRequest;
import com.example.schedulebook.domain.scheduleshare.dto.response.OwnedShareDetailResponse;
import com.example.schedulebook.domain.scheduleshare.dto.response.OwnedShareResponse;
import com.example.schedulebook.domain.scheduleshare.dto.response.ScheduleShareResponse;
import com.example.schedulebook.domain.scheduleshare.dto.response.SharedScheduleDetailResponse;
import com.example.schedulebook.domain.scheduleshare.dto.response.SharedScheduleResponse;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import com.example.schedulebook.domain.scheduleshare.validator.ScheduleShareValidator;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleShareServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FRIEND_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;

    private static final Long SCHEDULE_ID = 10L;
    private static final Long SHARE_ID = 100L;

    @Mock
    private ScheduleShareRepository scheduleShareRepository;

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    @Mock
    private ScheduleParticipantReader scheduleParticipantReader;

    @Mock
    private ScheduleAttendancePublisher scheduleAttendancePublisher;

    @Mock
    private ScheduleParticipantPublisher scheduleParticipantPublisher;

    @Mock
    private UserValidator userValidator;

    @Mock
    private ScheduleValidator scheduleValidator;

    @Mock
    private ScheduleShareValidator scheduleShareValidator;

    @Mock
    private FriendValidator friendValidator;

    @Mock
    private ScheduleParticipantValidator scheduleParticipantValidator;

    @Mock
    private OutboxService outboxService;

    private ScheduleShareService scheduleShareService;

    @BeforeEach
    void setUp() {
        scheduleShareService = new ScheduleShareService(
                scheduleShareRepository,
                scheduleParticipantRepository,
                scheduleParticipantReader,
                scheduleAttendancePublisher,
                scheduleParticipantPublisher,
                userValidator,
                scheduleValidator,
                scheduleShareValidator,
                friendValidator,
                scheduleParticipantValidator,
                outboxService
        );
    }

    // =========================================================
    // shareSchedule
    // =========================================================

    @Test
    void 일정_공유에_성공하면_공유_응답을_반환한다() {
        // given
        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);
        User currentUser = mock(User.class);
        User friendUser = mock(User.class);
        ScheduleShare scheduleShare = mock(ScheduleShare.class);

        ScheduleShareRequest request =
                new ScheduleShareRequest(FRIEND_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(friendUser.getId())
                .thenReturn(FRIEND_ID);

        when(friendUser.getNickname())
                .thenReturn("친구사용자");

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getNickname())
                .thenReturn("일정소유자");

        when(scheduleShareRepository.findRelation(
                SCHEDULE_ID,
                FRIEND_ID
        )).thenReturn(Optional.empty());

        when(scheduleShareRepository.save(any(ScheduleShare.class)))
                .thenReturn(scheduleShare);

        when(scheduleParticipantRepository.findParticipantIds(SCHEDULE_ID))
                .thenReturn(List.of());

        when(scheduleShare.getId())
                .thenReturn(SHARE_ID);

        when(scheduleShare.getSchedule())
                .thenReturn(schedule);

        when(scheduleShare.getSharedUser())
                .thenReturn(friendUser);

        try (
                MockedStatic<ScheduleShare> scheduleShareMock =
                        mockStatic(ScheduleShare.class)
        ) {
            scheduleShareMock
                    .when(() -> ScheduleShare.create(
                            schedule,
                            friendUser
                    ))
                    .thenReturn(scheduleShare);

            // when
            ScheduleShareResponse result =
                    scheduleShareService.shareSchedule(
                            SCHEDULE_ID,
                            request,
                            USER_ID
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.shareId())
                    .isEqualTo(SHARE_ID);

            assertThat(result.scheduleId())
                    .isEqualTo(SCHEDULE_ID);

            assertThat(result.sharedUserId())
                    .isEqualTo(FRIEND_ID);

            assertThat(result.sharedUserNickname())
                    .isEqualTo("친구사용자");

            verify(userValidator)
                    .validateActiveUser(USER_ID);

            verify(scheduleValidator)
                    .validateSchedule(
                            SCHEDULE_ID,
                            USER_ID
                    );

            verify(userValidator)
                    .validateActiveUser(FRIEND_ID);

            verify(userValidator)
                    .validateShareMyself(
                            USER_ID,
                            FRIEND_ID
                    );

            verify(friendValidator)
                    .validateFriendRelation(
                            USER_ID,
                            FRIEND_ID
                    );

            verify(scheduleShareRepository)
                    .findRelation(
                            SCHEDULE_ID,
                            FRIEND_ID
                    );

            verify(scheduleShareRepository)
                    .save(scheduleShare);

            verify(scheduleParticipantRepository)
                    .findParticipantIds(SCHEDULE_ID);

            verify(scheduleParticipantRepository)
                    .saveAll(any());

            verify(outboxService)
                    .save(
                            any(String.class),
                            eq(OutboxAggregateType.SCHEDULE),
                            eq(String.valueOf(SCHEDULE_ID)),
                            eq(OutboxEventType.SCHEDULE_SHARED),
                            any()
                    );
        }
    }

    @Test
    void 자기_자신에게_일정을_공유하면_검증에서_예외가_발생하고_공유하지_않는다() {
        // given
        Schedule schedule = mock(Schedule.class);
        User user = mock(User.class);

        ScheduleShareRequest request =
                new ScheduleShareRequest(USER_ID);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(user.getId())
                .thenReturn(USER_ID);

        doThrow(new RuntimeException("cannot share to myself"))
                .when(userValidator)
                .validateShareMyself(USER_ID, USER_ID);

        // when & then
        assertThatThrownBy(() ->
                scheduleShareService.shareSchedule(
                        SCHEDULE_ID,
                        request,
                        USER_ID
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cannot share to myself");

        verify(userValidator, times(2))
                .validateActiveUser(USER_ID);

        verify(userValidator)
                .validateShareMyself(USER_ID, USER_ID);

        verifyNoInteractions(friendValidator);
        verifyNoInteractions(scheduleShareRepository);
        verifyNoInteractions(scheduleParticipantRepository);
        verifyNoInteractions(outboxService);
    }

    @Test
    void 친구가_아닌_사용자에게_일정을_공유하면_검증에서_예외가_발생한다() {
        // given
        Schedule schedule = mock(Schedule.class);
        User friendUser = mock(User.class);

        ScheduleShareRequest request =
                new ScheduleShareRequest(OTHER_USER_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(userValidator.validateActiveUser(OTHER_USER_ID))
                .thenReturn(friendUser);

        when(friendUser.getId())
                .thenReturn(OTHER_USER_ID);

        doThrow(new RuntimeException("not friend"))
                .when(friendValidator)
                .validateFriendRelation(
                        USER_ID,
                        OTHER_USER_ID
                );

        // when & then
        assertThatThrownBy(() ->
                scheduleShareService.shareSchedule(
                        SCHEDULE_ID,
                        request,
                        USER_ID
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("not friend");

        verify(friendValidator)
                .validateFriendRelation(
                        USER_ID,
                        OTHER_USER_ID
                );

        verify(scheduleShareRepository, never())
                .findRelation(any(), any());

        verifyNoInteractions(outboxService);
    }

    @Test
    void 이미_공유된_일정이면_기존_공유를_재공유하고_응답을_반환한다() {
        // given
        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);
        User friendUser = mock(User.class);
        ScheduleShare existingShare = mock(ScheduleShare.class);
        ScheduleShareResponse response = mock(ScheduleShareResponse.class);

        ScheduleShareRequest request =
                new ScheduleShareRequest(FRIEND_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(friendUser.getId())
                .thenReturn(FRIEND_ID);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getNickname())
                .thenReturn("일정소유자");

        when(scheduleShareRepository.findRelation(
                SCHEDULE_ID,
                FRIEND_ID
        )).thenReturn(Optional.of(existingShare));

        try (
                MockedStatic<ScheduleShareResponse> responseMock =
                        mockStatic(ScheduleShareResponse.class)
        ) {
            responseMock
                    .when(() -> ScheduleShareResponse.from(existingShare))
                    .thenReturn(response);

            // when
            ScheduleShareResponse result =
                    scheduleShareService.shareSchedule(
                            SCHEDULE_ID,
                            request,
                            USER_ID
                    );

            // then
            assertThat(result)
                    .isSameAs(response);

            verify(scheduleShareValidator)
                    .validateShareStatus(existingShare);

            verify(existingShare)
                    .reShare();

            verify(scheduleShareRepository, never())
                    .save(any());

            verify(scheduleParticipantRepository, never())
                    .saveAll(any());

            verify(outboxService)
                    .save(
                            any(String.class),
                            eq(OutboxAggregateType.SCHEDULE),
                            eq(String.valueOf(SCHEDULE_ID)),
                            eq(OutboxEventType.SCHEDULE_SHARED),
                            any()
                    );
        }
    }

    @Test
    void 공유_생성_중_중복키가_발생하면_이미_생성된_공유를_재사용한다() {
        // given
        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);
        User friendUser = mock(User.class);
        ScheduleShare scheduleShare = mock(ScheduleShare.class);
        ScheduleShare alreadyCreated = mock(ScheduleShare.class);
        ScheduleShareResponse response = mock(ScheduleShareResponse.class);

        ScheduleShareRequest request =
                new ScheduleShareRequest(FRIEND_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(friendUser.getId())
                .thenReturn(FRIEND_ID);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getNickname())
                .thenReturn("일정소유자");

        when(scheduleShareRepository.findRelation(
                SCHEDULE_ID,
                FRIEND_ID
        )).thenReturn(
                Optional.empty(),
                Optional.of(alreadyCreated)
        );

        when(scheduleShareRepository.save(scheduleShare))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        try (
                MockedStatic<ScheduleShare> scheduleShareMock =
                        mockStatic(ScheduleShare.class);
                MockedStatic<ScheduleShareResponse> responseMock =
                        mockStatic(ScheduleShareResponse.class)
        ) {
            scheduleShareMock
                    .when(() -> ScheduleShare.create(
                            schedule,
                            friendUser
                    ))
                    .thenReturn(scheduleShare);

            responseMock
                    .when(() -> ScheduleShareResponse.from(alreadyCreated))
                    .thenReturn(response);

            // when
            ScheduleShareResponse result =
                    scheduleShareService.shareSchedule(
                            SCHEDULE_ID,
                            request,
                            USER_ID
                    );

            // then
            assertThat(result)
                    .isSameAs(response);

            verify(scheduleShareValidator)
                    .validateShareStatus(alreadyCreated);

            verify(alreadyCreated)
                    .reShare();

            verify(outboxService)
                    .save(
                            any(String.class),
                            eq(OutboxAggregateType.SCHEDULE),
                            eq(String.valueOf(SCHEDULE_ID)),
                            eq(OutboxEventType.SCHEDULE_SHARED),
                            any()
                    );
        }
    }

    @Test
    void 이미_활성화된_공유가_존재하면_일정_공유에_실패한다() {
        // given
        Schedule schedule = mock(Schedule.class);
        User friendUser = mock(User.class);
        ScheduleShare existingShare = mock(ScheduleShare.class);

        ScheduleShareRequest request =
                new ScheduleShareRequest(FRIEND_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(friendUser.getId())
                .thenReturn(FRIEND_ID);

        when(scheduleShareRepository.findRelation(
                SCHEDULE_ID,
                FRIEND_ID
        )).thenReturn(Optional.of(existingShare));

        doThrow(new BaseException(ErrorEnum.SCHEDULE_ALREADY_SHARED))
                .when(scheduleShareValidator)
                .validateShareStatus(existingShare);

        // when & then
        assertThatThrownBy(() ->
                scheduleShareService.shareSchedule(
                        SCHEDULE_ID,
                        request,
                        USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_ALREADY_SHARED);

        verify(scheduleShareValidator)
                .validateShareStatus(existingShare);

        verify(existingShare, never())
                .reShare();

        verify(scheduleShareRepository, never())
                .save(any());

        verify(scheduleParticipantRepository, never())
                .saveAll(any());

        verifyNoInteractions(outboxService);
    }

    @Test
    void 이미_참가중인_사용자에게_일정을_공유하면_중복_참가자를_생성하지_않는다() {
        // given
        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);
        User friendUser = mock(User.class);
        ScheduleShare scheduleShare = mock(ScheduleShare.class);

        ScheduleShareRequest request =
                new ScheduleShareRequest(FRIEND_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(userValidator.validateActiveUser(FRIEND_ID))
                .thenReturn(friendUser);

        when(friendUser.getId())
                .thenReturn(FRIEND_ID);

        when(friendUser.getNickname())
                .thenReturn("친구사용자");

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getNickname())
                .thenReturn("일정소유자");

        when(scheduleShareRepository.findRelation(
                SCHEDULE_ID,
                FRIEND_ID
        )).thenReturn(Optional.empty());

        when(scheduleShareRepository.save(any(ScheduleShare.class)))
                .thenReturn(scheduleShare);

        // 이미 친구가 참가자
        when(scheduleParticipantRepository.findParticipantIds(SCHEDULE_ID))
                .thenReturn(List.of(FRIEND_ID));

        when(scheduleShare.getId())
                .thenReturn(SHARE_ID);

        when(scheduleShare.getSchedule())
                .thenReturn(schedule);

        when(scheduleShare.getSharedUser())
                .thenReturn(friendUser);

        try (
                MockedStatic<ScheduleShare> scheduleShareMock =
                        mockStatic(ScheduleShare.class)
        ) {
            scheduleShareMock
                    .when(() -> ScheduleShare.create(
                            schedule,
                            friendUser
                    ))
                    .thenReturn(scheduleShare);

            // when
            ScheduleShareResponse result =
                    scheduleShareService.shareSchedule(
                            SCHEDULE_ID,
                            request,
                            USER_ID
                    );

            // then
            assertThat(result.shareId())
                    .isEqualTo(SHARE_ID);

            verify(scheduleParticipantRepository)
                    .findParticipantIds(SCHEDULE_ID);

            verify(scheduleParticipantRepository)
                    .saveAll(argThat(participants -> !participants.iterator().hasNext()));

            verify(outboxService)
                    .save(
                            any(String.class),
                            eq(OutboxAggregateType.SCHEDULE),
                            eq(String.valueOf(SCHEDULE_ID)),
                            eq(OutboxEventType.SCHEDULE_SHARED),
                            any()
                    );
        }
    }

    // =========================================================
    // findAllSharedSchedules
    // =========================================================

    @Test
    void 공유받은_일정_목록을_조회하면_ACTIVE_공유만_조회한다() {
        // given
        ScheduleShare share = mock(ScheduleShare.class);
        SharedScheduleResponse response = mock(SharedScheduleResponse.class);

        when(scheduleShareRepository.findSharedSchedules(
                USER_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(List.of(share));

        try (
                MockedStatic<SharedScheduleResponse> responseMock =
                        mockStatic(SharedScheduleResponse.class)
        ) {
            responseMock
                    .when(() -> SharedScheduleResponse.from(share))
                    .thenReturn(response);

            // when
            List<SharedScheduleResponse> result =
                    scheduleShareService.findAllSharedSchedules(USER_ID);

            // then
            assertThat(result)
                    .containsExactly(response);

            verify(userValidator)
                    .validateActiveUser(USER_ID);

            verify(scheduleShareRepository)
                    .findSharedSchedules(
                            USER_ID,
                            ScheduleShareStatus.ACTIVE
                    );
        }
    }

    // =========================================================
    // findOneSharedSchedule
    // =========================================================

    @Test
    void 공유받은_일정_상세를_조회하면_참가자정보를_포함한_상세응답을_반환한다() {
        // given
        ScheduleShare share = mock(ScheduleShare.class);
        Schedule schedule = mock(Schedule.class);

        SharedScheduleDetailResponse response =
                mock(SharedScheduleDetailResponse.class);

        ScheduleParticipantInfo info =
                mock(ScheduleParticipantInfo.class);

        when(scheduleShareValidator.validateScheduleShare(SHARE_ID))
                .thenReturn(share);

        when(share.getSchedule())
                .thenReturn(schedule);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleParticipantReader.getParticipantInfo(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(info);

        try (
                MockedStatic<SharedScheduleDetailResponse> responseMock =
                        mockStatic(SharedScheduleDetailResponse.class)
        ) {
            responseMock
                    .when(() -> SharedScheduleDetailResponse.from(
                            eq(share),
                            any(Boolean.class),
                            any(Integer.class),
                            any(List.class)
                    ))
                    .thenReturn(response);

            // when
            SharedScheduleDetailResponse result =
                    scheduleShareService.findOneSharedSchedule(
                            SHARE_ID,
                            USER_ID
                    );

            // then
            assertThat(result)
                    .isSameAs(response);

            verify(userValidator)
                    .validateActiveUser(USER_ID);

            verify(scheduleShareValidator)
                    .validateScheduleShare(SHARE_ID);

            verify(scheduleShareValidator)
                    .validateSharedUser(
                            share,
                            USER_ID
                    );

            verify(scheduleValidator)
                    .validateAccessibleSchedule(
                            SCHEDULE_ID,
                            USER_ID
                    );

            verify(scheduleParticipantReader)
                    .getParticipantInfo(
                            SCHEDULE_ID,
                            USER_ID
                    );
        }
    }

    @Test
    void 공유받지_않은_사용자가_공유_일정_상세를_조회하면_예외가_발생한다() {
        // given
        ScheduleShare share = mock(ScheduleShare.class);

        when(scheduleShareValidator.validateScheduleShare(SHARE_ID))
                .thenReturn(share);

        doThrow(new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN))
                .when(scheduleShareValidator)
                .validateSharedUser(
                        share,
                        USER_ID
                );

        // when & then
        assertThatThrownBy(() ->
                scheduleShareService.findOneSharedSchedule(
                        SHARE_ID,
                        USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(scheduleShareValidator)
                .validateSharedUser(
                        share,
                        USER_ID
                );

        verify(scheduleValidator, never())
                .validateAccessibleSchedule(any(), any());

        verifyNoInteractions(scheduleParticipantReader);
    }

    // =========================================================
    // findAllOwnedShares
    // =========================================================

    @Test
    void 내가_공유한_일정_목록을_조회하면_ACTIVE_공유만_조회한다() {
        // given
        ScheduleShare share = mock(ScheduleShare.class);
        OwnedShareResponse response = mock(OwnedShareResponse.class);

        when(scheduleShareRepository.findOwnedShares(
                USER_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(List.of(share));

        try (
                MockedStatic<OwnedShareResponse> responseMock =
                        mockStatic(OwnedShareResponse.class)
        ) {
            responseMock
                    .when(() -> OwnedShareResponse.from(share))
                    .thenReturn(response);

            // when
            List<OwnedShareResponse> result =
                    scheduleShareService.findAllOwnedShares(USER_ID);

            // then
            assertThat(result)
                    .containsExactly(response);

            verify(userValidator)
                    .validateActiveUser(USER_ID);

            verify(scheduleShareRepository)
                    .findOwnedShares(
                            USER_ID,
                            ScheduleShareStatus.ACTIVE
                    );
        }
    }

    // =========================================================
    // findOneOwnedShareDetail
    // =========================================================

    @Test
    void 내가_공유한_일정_상세를_조회하면_상세정보를_반환한다() {
        // given
        ScheduleShare share = mock(ScheduleShare.class);
        Schedule schedule = mock(Schedule.class);

        OwnedShareDetailResponse response =
                mock(OwnedShareDetailResponse.class);

        ScheduleParticipantInfo info =
                mock(ScheduleParticipantInfo.class);

        when(scheduleShareValidator.validateOwnedShare(SHARE_ID))
                .thenReturn(share);

        when(share.getSchedule())
                .thenReturn(schedule);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleParticipantReader.getParticipantInfo(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(info);

        try (
                MockedStatic<OwnedShareDetailResponse> responseMock =
                        mockStatic(OwnedShareDetailResponse.class)
        ) {
            responseMock
                    .when(() -> OwnedShareDetailResponse.from(
                            eq(share),
                            any(Boolean.class),
                            any(Integer.class),
                            any(List.class)
                    ))
                    .thenReturn(response);

            // when
            OwnedShareDetailResponse result =
                    scheduleShareService.findOneOwnedShareDetail(
                            SHARE_ID,
                            USER_ID
                    );

            // then
            assertThat(result)
                    .isSameAs(response);

            verify(userValidator)
                    .validateActiveUser(USER_ID);

            verify(scheduleShareValidator)
                    .validateOwnedShare(SHARE_ID);

            verify(scheduleShareValidator)
                    .validateShareOwner(
                            share,
                            USER_ID
                    );

            verify(scheduleParticipantReader)
                    .getParticipantInfo(
                            SCHEDULE_ID,
                            USER_ID
                    );
        }
    }

    @Test
    void 소유자가_아닌_사용자가_공유한_일정_상세를_조회하면_예외가_발생한다() {
        // given
        ScheduleShare share = mock(ScheduleShare.class);

        when(scheduleShareValidator.validateOwnedShare(SHARE_ID))
                .thenReturn(share);

        doThrow(new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN))
                .when(scheduleShareValidator)
                .validateShareOwner(
                        share,
                        USER_ID
                );

        // when & then
        assertThatThrownBy(() ->
                scheduleShareService.findOneOwnedShareDetail(
                        SHARE_ID,
                        USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(scheduleShareValidator)
                .validateShareOwner(
                        share,
                        USER_ID
                );

        verifyNoInteractions(scheduleParticipantReader);
    }

    // =========================================================
    // findParticipants
    // =========================================================

    @Test
    void 일정_참가자_목록을_조회하면_접근가능한_일정의_참가자목록을_반환한다() {
        // given
        ScheduleParticipantListResponse response =
                mock(ScheduleParticipantListResponse.class);

        when(scheduleParticipantReader.getParticipantList(SCHEDULE_ID))
                .thenReturn(response);

        // when
        ScheduleParticipantListResponse result =
                scheduleShareService.findParticipants(
                        USER_ID,
                        SCHEDULE_ID
                );

        // then
        assertThat(result)
                .isSameAs(response);

        verify(userValidator)
                .validateActiveUser(USER_ID);

        verify(scheduleValidator)
                .validateAccessibleSchedule(
                        SCHEDULE_ID,
                        USER_ID
                );

        verify(scheduleParticipantReader)
                .getParticipantList(SCHEDULE_ID);
    }

    @Test
    void 접근할_수_없는_일정의_참가자_목록을_조회하면_예외가_발생한다() {
        // given
        doThrow(new RuntimeException("not accessible"))
                .when(scheduleValidator)
                .validateAccessibleSchedule(
                        SCHEDULE_ID,
                        USER_ID
                );

        // when & then
        assertThatThrownBy(() ->
                scheduleShareService.findParticipants(
                        USER_ID,
                        SCHEDULE_ID
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("not accessible");

        verifyNoInteractions(scheduleParticipantReader);
    }

    // =========================================================
    // updateAttendance
    // =========================================================

    @Test
    void 참석상태를_변경하면_참석상태와_참가자목록_이벤트를_발행한다() {
        // given
        User user = mock(User.class);
        ScheduleParticipant participant = mock(ScheduleParticipant.class);

        UpdateAttendanceRequest request =
                new UpdateAttendanceRequest(
                        AttendanceStatus.ACCEPTED
                );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(user.getNickname())
                .thenReturn("사용자");

        when(scheduleParticipantValidator.validateParticipant(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(participant);

        when(participant.getAttendanceStatus())
                .thenReturn(AttendanceStatus.PENDING);

        // when
        scheduleShareService.updateAttendance(
                USER_ID,
                SCHEDULE_ID,
                request
        );

        // then
        verify(userValidator)
                .validateActiveUser(USER_ID);

        verify(scheduleValidator)
                .validateAccessibleSchedule(
                        SCHEDULE_ID,
                        USER_ID
                );

        verify(scheduleParticipantValidator)
                .validateParticipant(
                        SCHEDULE_ID,
                        USER_ID
                );

        verify(participant)
                .updateAttendanceStatus(
                        AttendanceStatus.ACCEPTED
                );

        verify(scheduleAttendancePublisher)
                .publishAttendanceUpdated(
                        any(ScheduleAttendanceResponse.class)
                );

        verify(scheduleParticipantPublisher)
                .publishParticipantsUpdated(SCHEDULE_ID);
    }

    @Test
    void 기존_참석상태와_동일한_상태로_수정하면_상태를_변경하지_않고_이벤트도_발행하지_않는다() {
        // given
        User user = mock(User.class);
        ScheduleParticipant participant = mock(ScheduleParticipant.class);

        UpdateAttendanceRequest request =
                new UpdateAttendanceRequest(
                        AttendanceStatus.ACCEPTED
                );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleParticipantValidator.validateParticipant(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(participant);

        when(participant.getAttendanceStatus())
                .thenReturn(AttendanceStatus.ACCEPTED);

        // when
        scheduleShareService.updateAttendance(
                USER_ID,
                SCHEDULE_ID,
                request
        );

        // then
        verify(participant, never())
                .updateAttendanceStatus(any());

        verifyNoInteractions(scheduleAttendancePublisher);
        verifyNoInteractions(scheduleParticipantPublisher);
    }

    // =========================================================
    // cancelShare
    // =========================================================

    @Test
    void 일정_공유를_취소하면_공유를_취소하고_Outbox_이벤트를_저장한다() {
        // given
        Schedule schedule = mock(Schedule.class);
        User sharedUser = mock(User.class);
        ScheduleShare share = mock(ScheduleShare.class);

        when(scheduleShareValidator.validateActiveScheduleShare(SHARE_ID))
                .thenReturn(share);

        when(share.getSchedule())
                .thenReturn(schedule);

        when(share.getSharedUser())
                .thenReturn(sharedUser);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(sharedUser.getId())
                .thenReturn(FRIEND_ID);

        // when
        scheduleShareService.cancelShare(
                SHARE_ID,
                USER_ID
        );

        // then
        verify(userValidator)
                .validateActiveUser(USER_ID);

        verify(scheduleShareValidator)
                .validateActiveScheduleShare(SHARE_ID);

        verify(scheduleValidator)
                .validateScheduleOwner(
                        schedule,
                        USER_ID
                );

        verify(share)
                .cancelShare();

        verify(outboxService)
                .save(
                        any(String.class),
                        eq(OutboxAggregateType.SCHEDULE),
                        eq(String.valueOf(SCHEDULE_ID)),
                        eq(OutboxEventType.SCHEDULE_CANCELED),
                        any()
                );
    }

    @Test
    void 일정_소유자가_아닌_사용자가_공유를_취소하면_예외가_발생하고_Outbox를_저장하지_않는다() {
        // given
        Schedule schedule = mock(Schedule.class);
        ScheduleShare share = mock(ScheduleShare.class);

        when(scheduleShareValidator.validateActiveScheduleShare(SHARE_ID))
                .thenReturn(share);

        when(share.getSchedule())
                .thenReturn(schedule);

        doThrow(new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN))
                .when(scheduleValidator)
                .validateScheduleOwner(
                        schedule,
                        USER_ID
                );

        // when & then
        assertThatThrownBy(() ->
                scheduleShareService.cancelShare(
                        SHARE_ID,
                        USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(share, never())
                .cancelShare();

        verifyNoInteractions(outboxService);
    }

    @Test
    void 이미_취소된_공유는_다시_취소할_수_없다() {
        // given
        when(scheduleShareValidator.validateActiveScheduleShare(SHARE_ID))
                .thenThrow(new BaseException(
                        ErrorEnum.INVALID_SCHEDULE_SHARE_STATUS
                ));

        // when & then
        assertThatThrownBy(() ->
                scheduleShareService.cancelShare(
                        SHARE_ID,
                        USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_SCHEDULE_SHARE_STATUS);

        verify(scheduleValidator, never())
                .validateScheduleOwner(any(), eq(USER_ID));

        verifyNoInteractions(outboxService);
    }

    // =========================================================
    // deleteAllShared
    // =========================================================

    @Test
    void 사용자_삭제시_공유받은_일정을_삭제하고_소유한_공유를_소프트삭제한다() {
        // when
        scheduleShareService.deleteAllShared(USER_ID);

        // then
        verify(scheduleShareRepository)
                .deleteAllBySharedUserId(USER_ID);

        verify(scheduleShareRepository)
                .softDeleteOwnedShares(USER_ID);

        verifyNoMoreInteractions(scheduleShareRepository);
    }
}