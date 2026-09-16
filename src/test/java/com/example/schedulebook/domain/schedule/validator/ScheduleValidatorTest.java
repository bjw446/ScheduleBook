package com.example.schedulebook.domain.schedule.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleValidatorTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long SCHEDULE_ID = 100L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleShareRepository scheduleShareRepository;

    @Mock
    private User owner;

    @Mock
    private ScheduleShare scheduleShare;

    private ScheduleValidator scheduleValidator;

    @BeforeEach
    void setUp() {
        scheduleValidator = new ScheduleValidator(
                scheduleRepository,
                scheduleShareRepository
        );
    }

    @Test
    void 일정_조회_정상() {
        // given
        Schedule schedule = createSchedule(owner);

        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        // when
        Schedule result = scheduleValidator.findSchedule(SCHEDULE_ID);

        // then
        assertThat(result)
                .isSameAs(schedule);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 일정_조회_일정없으면_예외() {
        // given
        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.findSchedule(SCHEDULE_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_NOT_FOUND);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 일정_검증_소유자_정상() {
        // given
        when(owner.getId()).thenReturn(USER_ID);

        Schedule schedule = createSchedule(owner);

        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        // when
        Schedule result = scheduleValidator.validateSchedule(
                SCHEDULE_ID,
                owner.getId()
        );

        // then
        assertThat(result)
                .isSameAs(schedule);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 일정_검증_소유자가아니면_예외() {
        // given
        Schedule schedule = createSchedule(owner);

        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.validateSchedule(
                        SCHEDULE_ID,
                        OTHER_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 일정_검증_일정없으면_예외() {
        // given
        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.validateSchedule(
                        SCHEDULE_ID,
                        USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_NOT_FOUND);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 일정_접근검증_소유자_정상() {
        // given
        when(owner.getId()).thenReturn(USER_ID);

        Schedule schedule = createSchedule(owner);

        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        // when
        Schedule result = scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                owner.getId()
        );

        // then
        assertThat(result)
                .isSameAs(schedule);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 일정_접근검증_공유사용자_ACTIVE_정상() {
        // given
        Schedule schedule = createSchedule(owner);

        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(scheduleShareRepository.findActiveRelation(
                eq(SCHEDULE_ID),
                eq(OTHER_USER_ID),
                eq(ScheduleShareStatus.ACTIVE)
        )).thenReturn(Optional.of(scheduleShare));

        // when
        Schedule result = scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                OTHER_USER_ID
        );

        // then
        assertThat(result)
                .isSameAs(schedule);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verify(scheduleShareRepository)
                .findActiveRelation(
                        SCHEDULE_ID,
                        OTHER_USER_ID,
                        ScheduleShareStatus.ACTIVE
                );
    }

    @Test
    void 일정_접근검증_공유관계없으면_예외() {
        // given
        Schedule schedule = createSchedule(owner);

        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(scheduleShareRepository.findActiveRelation(
                eq(SCHEDULE_ID),
                eq(OTHER_USER_ID),
                eq(ScheduleShareStatus.ACTIVE)
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.validateAccessibleSchedule(
                        SCHEDULE_ID,
                        OTHER_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verify(scheduleShareRepository)
                .findActiveRelation(
                        SCHEDULE_ID,
                        OTHER_USER_ID,
                        ScheduleShareStatus.ACTIVE
                );
    }

    @Test
    void 일정_접근검증_일정없으면_예외() {
        // given
        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.validateAccessibleSchedule(
                        SCHEDULE_ID,
                        OTHER_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_NOT_FOUND);

        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 연월_검증_정상() {
        // when & then
        scheduleValidator.validateYearMonth(2026, 9);

        verifyNoInteractions(
                scheduleRepository,
                scheduleShareRepository
        );
    }

    @Test
    void 연월_검증_월이_0이면_예외() {
        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.validateYearMonth(2026, 0)
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_SCHEDULE_MONTH);

        verifyNoInteractions(
                scheduleRepository,
                scheduleShareRepository
        );
    }

    @Test
    void 연월_검증_월이_13이면_예외() {
        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.validateYearMonth(2026, 13)
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_SCHEDULE_MONTH);

        verifyNoInteractions(
                scheduleRepository,
                scheduleShareRepository
        );
    }

    @Test
    void 연월_검증_연도가_범위를벗어나면_예외() {
        // java.time.YearMonth가 허용하는 범위 밖의 연도
        int invalidYear = 1_000_000_000;

        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.validateYearMonth(
                        invalidYear,
                        1
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.INVALID_SCHEDULE_MONTH);

        verifyNoInteractions(
                scheduleRepository,
                scheduleShareRepository
        );
    }

    @Test
    void 일정_소유자_검증_소유자_정상() {
        // given
        when(owner.getId()).thenReturn(USER_ID);

        Schedule schedule = createSchedule(owner);

        // when & then
        scheduleValidator.validateScheduleOwner(
                schedule,
                owner.getId()
        );

        verifyNoInteractions(
                scheduleRepository,
                scheduleShareRepository
        );
    }

    @Test
    void 일정_소유자_검증_다른사용자면_예외() {
        // given
        Schedule schedule = createSchedule(owner);

        // when & then
        assertThatThrownBy(() ->
                scheduleValidator.validateScheduleOwner(
                        schedule,
                        OTHER_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verifyNoInteractions(
                scheduleRepository,
                scheduleShareRepository
        );
    }

    private Schedule createSchedule(User scheduleOwner) {
        Schedule schedule = Schedule.create(
                scheduleOwner,
                "테스트 일정",
                "테스트 내용",
                LocalDate.of(2026, 9, 16),
                LocalTime.of(10, 30),
                LocalTime.of(11, 30)
        );

        ReflectionTestUtils.setField(
                schedule,
                "id",
                SCHEDULE_ID
        );

        return schedule;
    }
}