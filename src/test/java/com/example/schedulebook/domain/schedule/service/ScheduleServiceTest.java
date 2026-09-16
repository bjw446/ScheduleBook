package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.schedule.dto.request.CreateScheduleRequest;
import com.example.schedulebook.domain.schedule.dto.request.UpdateScheduleRequest;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleDetailResponse;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.event.ScheduleDeletedEvent;
import com.example.schedulebook.domain.schedule.event.ScheduleUpdatedEvent;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.schedule.validator.ScheduleValidator;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantResponse;
import com.example.schedulebook.domain.scheduleparticipant.enums.AttendanceStatus;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.scheduleparticipant.service.ScheduleParticipantReader;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long SCHEDULE_ID = 100L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    @Mock
    private ScheduleParticipantReader scheduleParticipantReader;

    @Mock
    private UserValidator userValidator;

    @Mock
    private ScheduleValidator scheduleValidator;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ScheduleReminderService scheduleReminderService;

    @Mock
    private User user;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                scheduleRepository,
                scheduleParticipantRepository,
                scheduleParticipantReader,
                userValidator,
                scheduleValidator,
                outboxService,
                scheduleReminderService
        );
    }

    @Test
    void 일정_생성_정상() {
        // given
        CreateScheduleRequest request = mock(CreateScheduleRequest.class);

        LocalDate scheduleDate = LocalDate.of(2026, 9, 16);
        LocalTime startTime = LocalTime.of(10, 30, 15);
        LocalTime endTime = LocalTime.of(11, 30, 20);

        when(request.title()).thenReturn("스터디");
        when(request.content()).thenReturn("Spring 공부");
        when(request.scheduleDate()).thenReturn(scheduleDate);
        when(request.startTime()).thenReturn(startTime);
        when(request.endTime()).thenReturn(endTime);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        Schedule savedSchedule = Schedule.create(
                user,
                "스터디",
                "Spring 공부",
                scheduleDate,
                startTime,
                endTime
        );

        setScheduleId(savedSchedule, SCHEDULE_ID);

        when(scheduleRepository.save(any(Schedule.class)))
                .thenReturn(savedSchedule);

        // when
        ScheduleSummaryResponse response =
                scheduleService.createSchedule(request, USER_ID);

        // then
        assertThat(response.scheduleId())
                .isEqualTo(SCHEDULE_ID);
        assertThat(response.title())
                .isEqualTo("스터디");
        assertThat(response.commentCount())
                .isZero();
        assertThat(response.scheduleDate())
                .isEqualTo(scheduleDate);

        ArgumentCaptor<Schedule> scheduleCaptor =
                ArgumentCaptor.forClass(Schedule.class);

        verify(scheduleRepository)
                .save(scheduleCaptor.capture());

        Schedule schedule = scheduleCaptor.getValue();

        assertThat(schedule.getUser())
                .isSameAs(user);
        assertThat(schedule.getTitle())
                .isEqualTo("스터디");
        assertThat(schedule.getContent())
                .isEqualTo("Spring 공부");
        assertThat(schedule.getScheduleDate())
                .isEqualTo(scheduleDate);

        // Schedule.create()에서 초와 나노초를 제거한다.
        assertThat(schedule.getStartTime())
                .isEqualTo(LocalTime.of(10, 30));
        assertThat(schedule.getEndTime())
                .isEqualTo(LocalTime.of(11, 30));

        assertThat(schedule.isStartTimeSpecified())
                .isTrue();
        assertThat(schedule.isEndTimeSpecified())
                .isTrue();
        assertThat(schedule.getScheduleVersion())
                .isEqualTo(1L);
        assertThat(schedule.getCommentCount())
                .isZero();

        verify(scheduleParticipantRepository)
                .save(any(ScheduleParticipant.class));

        verify(user)
                .increaseScheduleCount();

        verify(scheduleReminderService)
                .save(savedSchedule);
    }

    @Test
    void 일정_생성_사용자가_존재하지_않으면_예외() {
        // given
        CreateScheduleRequest request =
                mock(CreateScheduleRequest.class);

        BaseException exception =
                new BaseException(ErrorEnum.USER_NOT_FOUND);

        when(userValidator.validateActiveUser(USER_ID))
                .thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleService.createSchedule(request, USER_ID)
        )
                .isSameAs(exception);

        verifyNoInteractions(scheduleRepository);
        verifyNoInteractions(scheduleParticipantRepository);
        verifyNoInteractions(scheduleReminderService);
        verifyNoInteractions(user);
    }

    @Test
    void 일정_생성_리마인더_저장() {
        // given
        CreateScheduleRequest request =
                mock(CreateScheduleRequest.class);

        LocalDate scheduleDate =
                LocalDate.of(2026, 9, 16);

        LocalTime startTime =
                LocalTime.of(10, 30);

        LocalTime endTime =
                LocalTime.of(11, 30);

        when(request.title())
                .thenReturn("회의");
        when(request.content())
                .thenReturn("팀 회의");
        when(request.scheduleDate())
                .thenReturn(scheduleDate);
        when(request.startTime())
                .thenReturn(startTime);
        when(request.endTime())
                .thenReturn(endTime);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        Schedule savedSchedule = Schedule.create(
                user,
                "회의",
                "팀 회의",
                scheduleDate,
                startTime,
                endTime
        );

        setScheduleId(savedSchedule, SCHEDULE_ID);

        when(scheduleRepository.save(any(Schedule.class)))
                .thenReturn(savedSchedule);

        // when
        scheduleService.createSchedule(request, USER_ID);

        // then
        verify(scheduleReminderService)
                .save(savedSchedule);
    }

    @Test
    void 일정_단건_조회_소유자_정상() {
        // given
        Schedule schedule =
                createSchedule(SCHEDULE_ID);

        ScheduleParticipantResponse participant =
                new ScheduleParticipantResponse(
                        USER_ID,
                        "주원",
                        true,
                        AttendanceStatus.PENDING
                );

        ScheduleParticipantInfo info =
                ScheduleParticipantInfo.from(
                        true,
                        1,
                        List.of(participant)
                );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(scheduleParticipantReader.getParticipantInfo(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(info);

        // when
        ScheduleDetailResponse response =
                scheduleService.findOneSchedule(
                        SCHEDULE_ID,
                        USER_ID
                );

        // then
        assertThat(response.scheduleId())
                .isEqualTo(SCHEDULE_ID);
        assertThat(response.title())
                .isEqualTo("테스트 일정");
        assertThat(response.content())
                .isEqualTo("테스트 내용");
        assertThat(response.commentCount())
                .isZero();
        assertThat(response.scheduleDate())
                .isEqualTo(LocalDate.of(2026, 9, 16));
        assertThat(response.startTime())
                .isEqualTo(LocalTime.of(10, 30));
        assertThat(response.endTime())
                .isEqualTo(LocalTime.of(11, 30));
        assertThat(response.startTimeSpecified())
                .isTrue();
        assertThat(response.endTimeSpecified())
                .isTrue();
        assertThat(response.participated())
                .isTrue();
        assertThat(response.participantCount())
                .isEqualTo(1);
        assertThat(response.participants())
                .containsExactly(participant);

        verify(scheduleParticipantReader)
                .getParticipantInfo(SCHEDULE_ID, USER_ID);
    }

    @Test
    void 일정_단건_조회_공유사용자_현재구현에서는_접근거부() {
        // given
        BaseException exception =
                new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);

        when(userValidator.validateActiveUser(OTHER_USER_ID))
                .thenReturn(mock(User.class));

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                OTHER_USER_ID
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleService.findOneSchedule(
                        SCHEDULE_ID,
                        OTHER_USER_ID
                )
        )
                .isSameAs(exception);

        verify(scheduleParticipantReader, never())
                .getParticipantInfo(anyLong(), anyLong());
    }

    @Test
    void 일정_단건_조회_접근권한없는사용자_예외() {
        // given
        BaseException exception =
                new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);

        when(userValidator.validateActiveUser(OTHER_USER_ID))
                .thenReturn(mock(User.class));

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                OTHER_USER_ID
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleService.findOneSchedule(
                        SCHEDULE_ID,
                        OTHER_USER_ID
                )
        )
                .isSameAs(exception);

        verify(scheduleParticipantReader, never())
                .getParticipantInfo(anyLong(), anyLong());
    }

    @Test
    void 일정_단건_조회_일정없음() {
        // given
        BaseException exception =
                new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleService.findOneSchedule(
                        SCHEDULE_ID,
                        USER_ID
                )
        )
                .isSameAs(exception);

        verify(scheduleParticipantReader, never())
                .getParticipantInfo(anyLong(), anyLong());
    }

    @Test
    void 월별_일정조회_해당월_전체범위로_조회() {
        // given
        int year = 2026;
        int month = 9;

        Schedule firstSchedule =
                createSchedule(101L);

        Schedule lastSchedule =
                createSchedule(102L);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleRepository.findAllByUserIdAndScheduleDateBetween(
                eq(USER_ID),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(
                firstSchedule,
                lastSchedule
        ));

        // when
        List<ScheduleSummaryResponse> response =
                scheduleService.findSchedulesByMonth(
                        year,
                        month,
                        USER_ID
                );

        // then
        assertThat(response)
                .hasSize(2);

        ArgumentCaptor<LocalDate> startCaptor =
                ArgumentCaptor.forClass(LocalDate.class);

        ArgumentCaptor<LocalDate> endCaptor =
                ArgumentCaptor.forClass(LocalDate.class);

        verify(scheduleRepository)
                .findAllByUserIdAndScheduleDateBetween(
                        eq(USER_ID),
                        startCaptor.capture(),
                        endCaptor.capture()
                );

        assertThat(startCaptor.getValue())
                .isEqualTo(LocalDate.of(2026, 9, 1));

        assertThat(endCaptor.getValue())
                .isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void 월별_일정조회_다른사용자_일정은_조회하지않음() {
        // given
        int year = 2026;
        int month = 9;

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleRepository.findAllByUserIdAndScheduleDateBetween(
                eq(USER_ID),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of());

        // when
        List<ScheduleSummaryResponse> response =
                scheduleService.findSchedulesByMonth(
                        year,
                        month,
                        USER_ID
                );

        // then
        assertThat(response)
                .isEmpty();

        verify(scheduleRepository)
                .findAllByUserIdAndScheduleDateBetween(
                        eq(USER_ID),
                        eq(LocalDate.of(2026, 9, 1)),
                        eq(LocalDate.of(2026, 9, 30))
                );
    }

    @Test
    void 월별_일정조회_잘못된_연월이면_예외() {
        // given
        int year = 2026;
        int invalidMonth = 13;

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        BaseException exception =
                new BaseException(ErrorEnum.INVALID_SCHEDULE_MONTH);

        doThrow(exception)
                .when(scheduleValidator)
                .validateYearMonth(
                        year,
                        invalidMonth
                );

        // when & then
        assertThatThrownBy(() ->
                scheduleService.findSchedulesByMonth(
                        year,
                        invalidMonth,
                        USER_ID
                )
        )
                .isSameAs(exception);

        verify(scheduleRepository, never())
                .findAllByUserIdAndScheduleDateBetween(
                        anyLong(),
                        any(LocalDate.class),
                        any(LocalDate.class)
                );
    }

    @Test
    void 날짜별_일정조회_정상() {
        // given
        LocalDate date =
                LocalDate.of(2026, 9, 16);

        Schedule schedule =
                createSchedule(SCHEDULE_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleRepository.findByUser_IdAndScheduleDate(
                USER_ID,
                date
        )).thenReturn(List.of(schedule));

        // when
        List<ScheduleSummaryResponse> response =
                scheduleService.findSchedulesByDate(
                        date,
                        USER_ID
                );

        // then
        assertThat(response)
                .hasSize(1);

        assertThat(response.get(0).scheduleId())
                .isEqualTo(SCHEDULE_ID);

        assertThat(response.get(0).title())
                .isEqualTo("테스트 일정");

        verify(scheduleRepository)
                .findByUser_IdAndScheduleDate(
                        USER_ID,
                        date
                );
    }

    @Test
    void 날짜별_일정조회_다른사용자_일정은_조회하지않음() {
        // given
        LocalDate date =
                LocalDate.of(2026, 9, 16);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleRepository.findByUser_IdAndScheduleDate(
                USER_ID,
                date
        )).thenReturn(List.of());

        // when
        List<ScheduleSummaryResponse> response =
                scheduleService.findSchedulesByDate(
                        date,
                        USER_ID
                );

        // then
        assertThat(response)
                .isEmpty();

        verify(scheduleRepository)
                .findByUser_IdAndScheduleDate(
                        USER_ID,
                        date
                );
    }

    @Test
    void 일정_수정_정상() {
        // given
        Schedule schedule =
                createSchedule(SCHEDULE_ID);

        UpdateScheduleRequest request =
                mock(UpdateScheduleRequest.class);

        LocalDate newDate =
                LocalDate.of(2026, 9, 20);

        LocalTime newStartTime =
                LocalTime.of(14, 20);

        LocalTime newEndTime =
                LocalTime.of(15, 30);

        when(request.title())
                .thenReturn("수정된 일정");
        when(request.content())
                .thenReturn("수정된 내용");
        when(request.scheduleDate())
                .thenReturn(newDate);
        when(request.startTime())
                .thenReturn(newStartTime);
        when(request.endTime())
                .thenReturn(newEndTime);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        // when
        ScheduleSummaryResponse response =
                scheduleService.updateSchedule(
                        SCHEDULE_ID,
                        request,
                        USER_ID
                );

        // then
        assertThat(response.scheduleId())
                .isEqualTo(SCHEDULE_ID);

        assertThat(response.title())
                .isEqualTo("수정된 일정");

        assertThat(response.commentCount())
                .isZero();

        assertThat(response.scheduleDate())
                .isEqualTo(newDate);

        assertThat(schedule.getTitle())
                .isEqualTo("수정된 일정");

        assertThat(schedule.getContent())
                .isEqualTo("수정된 내용");

        assertThat(schedule.getScheduleDate())
                .isEqualTo(newDate);

        assertThat(schedule.getStartTime())
                .isEqualTo(newStartTime);

        assertThat(schedule.getEndTime())
                .isEqualTo(newEndTime);

        assertThat(schedule.getScheduleVersion())
                .isEqualTo(2L);

        verify(scheduleReminderService)
                .refresh(schedule);

        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Object> payloadCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(outboxService).save(
                eventIdCaptor.capture(),
                eq(OutboxAggregateType.SCHEDULE),
                eq(String.valueOf(SCHEDULE_ID)),
                eq(OutboxEventType.SCHEDULE_UPDATED),
                payloadCaptor.capture()
        );

        assertThat(eventIdCaptor.getValue())
                .isNotBlank();

        assertThat(payloadCaptor.getValue())
                .isInstanceOf(ScheduleUpdatedEvent.class);

        ScheduleUpdatedEvent event =
                (ScheduleUpdatedEvent) payloadCaptor.getValue();

        assertThat(event.eventId())
                .isEqualTo(eventIdCaptor.getValue());

        assertThat(event.scheduleId())
                .isEqualTo(SCHEDULE_ID);
    }

    @Test
    void 일정_수정_소유자가아니면_예외() {
        // given
        UpdateScheduleRequest request =
                mock(UpdateScheduleRequest.class);

        when(userValidator.validateActiveUser(OTHER_USER_ID))
                .thenReturn(mock(User.class));

        BaseException exception =
                new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                OTHER_USER_ID
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleService.updateSchedule(
                        SCHEDULE_ID,
                        request,
                        OTHER_USER_ID
                )
        )
                .isSameAs(exception);

        verifyNoInteractions(scheduleReminderService);
        verifyNoInteractions(outboxService);
    }

    @Test
    void 일정_수정_일정없으면_예외() {
        // given
        UpdateScheduleRequest request =
                mock(UpdateScheduleRequest.class);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        BaseException exception =
                new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleService.updateSchedule(
                        SCHEDULE_ID,
                        request,
                        USER_ID
                )
        )
                .isSameAs(exception);

        verifyNoInteractions(scheduleReminderService);
        verifyNoInteractions(outboxService);
    }

    @Test
    void 일정_수정_리마인더_갱신_후_Outbox_저장() {
        // given
        Schedule schedule =
                createSchedule(SCHEDULE_ID);

        UpdateScheduleRequest request =
                mock(UpdateScheduleRequest.class);

        when(request.title())
                .thenReturn("수정");

        when(request.content())
                .thenReturn("수정 내용");

        when(request.scheduleDate())
                .thenReturn(LocalDate.of(2026, 9, 17));

        when(request.startTime())
                .thenReturn(LocalTime.of(12, 0));

        when(request.endTime())
                .thenReturn(LocalTime.of(13, 0));

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        // when
        scheduleService.updateSchedule(
                SCHEDULE_ID,
                request,
                USER_ID
        );

        // then
        InOrder inOrder =
                inOrder(
                        scheduleReminderService,
                        outboxService
                );

        inOrder.verify(scheduleReminderService)
                .refresh(schedule);

        inOrder.verify(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.SCHEDULE),
                        eq(String.valueOf(SCHEDULE_ID)),
                        eq(OutboxEventType.SCHEDULE_UPDATED),
                        any(ScheduleUpdatedEvent.class)
                );
    }

    @Test
    void 일정_삭제_정상() {
        // given
        Schedule schedule =
                createSchedule(SCHEDULE_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        // when
        scheduleService.deleteSchedule(
                SCHEDULE_ID,
                USER_ID
        );

        // then
        assertThat(schedule.isDeleted())
                .isTrue();

        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Object> payloadCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(outboxService).save(
                eventIdCaptor.capture(),
                eq(OutboxAggregateType.SCHEDULE),
                eq(String.valueOf(SCHEDULE_ID)),
                eq(OutboxEventType.SCHEDULE_DELETED),
                payloadCaptor.capture()
        );

        assertThat(eventIdCaptor.getValue())
                .isNotBlank();

        assertThat(payloadCaptor.getValue())
                .isInstanceOf(ScheduleDeletedEvent.class);

        ScheduleDeletedEvent event =
                (ScheduleDeletedEvent) payloadCaptor.getValue();

        assertThat(event.eventId())
                .isEqualTo(eventIdCaptor.getValue());

        assertThat(event.scheduleId())
                .isEqualTo(SCHEDULE_ID);
    }

    @Test
    void 일정_삭제_소유자가아니면_예외() {
        // given
        when(userValidator.validateActiveUser(OTHER_USER_ID))
                .thenReturn(mock(User.class));

        BaseException exception =
                new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                OTHER_USER_ID
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleService.deleteSchedule(
                        SCHEDULE_ID,
                        OTHER_USER_ID
                )
        )
                .isSameAs(exception);

        verifyNoInteractions(outboxService);
    }

    @Test
    void 일정_삭제_일정없으면_예외() {
        // given
        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        BaseException exception =
                new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND);

        when(scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                scheduleService.deleteSchedule(
                        SCHEDULE_ID,
                        USER_ID
                )
        )
                .isSameAs(exception);

        verifyNoInteractions(outboxService);
    }

    @Test
    void 전체_일정삭제_정상() {
        // when
        scheduleService.deleteAllSchedules(USER_ID);

        // then
        InOrder inOrder =
                inOrder(
                        scheduleParticipantRepository,
                        scheduleRepository
                );

        inOrder.verify(scheduleParticipantRepository)
                .deleteAllByUserId(USER_ID);

        inOrder.verify(scheduleParticipantRepository)
                .deleteByOwnedSchedule(USER_ID);

        inOrder.verify(scheduleRepository)
                .softDeleteAllByUserId(USER_ID);
    }

    @Test
    void 전체_일정삭제_지정한_사용자_ID로_처리() {
        // given
        Long targetUserId = 999L;

        // when
        scheduleService.deleteAllSchedules(targetUserId);

        // then
        verify(scheduleParticipantRepository)
                .deleteAllByUserId(targetUserId);

        verify(scheduleParticipantRepository)
                .deleteByOwnedSchedule(targetUserId);

        verify(scheduleRepository)
                .softDeleteAllByUserId(targetUserId);

        verifyNoMoreInteractions(
                scheduleParticipantRepository,
                scheduleRepository
        );
    }

    private Schedule createSchedule(Long scheduleId) {
        User owner = mock(User.class);

        Schedule schedule = Schedule.create(
                owner,
                "테스트 일정",
                "테스트 내용",
                LocalDate.of(2026, 9, 16),
                LocalTime.of(10, 30),
                LocalTime.of(11, 30)
        );

        setScheduleId(schedule, scheduleId);

        return schedule;
    }

    private void setScheduleId(
            Schedule schedule,
            Long scheduleId
    ) {
        try {
            Field field =
                    Schedule.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(schedule, scheduleId);

        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "Schedule ID 설정 실패",
                    e
            );
        }
    }
}