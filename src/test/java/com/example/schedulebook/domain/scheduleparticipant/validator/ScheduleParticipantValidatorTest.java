package com.example.schedulebook.domain.scheduleparticipant.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleParticipantValidatorTest {

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    @Mock
    private ScheduleShareRepository scheduleShareRepository;

    @Mock
    private ScheduleParticipant scheduleParticipant;

    private ScheduleParticipantValidator validator;

    private static final Long SCHEDULE_ID = 1L;
    private static final Long CURRENT_USER_ID = 10L;

    @BeforeEach
    void setUp() {
        validator = new ScheduleParticipantValidator(
                scheduleParticipantRepository,
                scheduleShareRepository
        );
    }

    @Test
    void 이미_참여한_사용자가_참여를_시도하면_이미_참여한_예외가_발생한다() {
        // given
        when(scheduleParticipantRepository.existsBySchedule_IdAndUser_Id(
                SCHEDULE_ID,
                CURRENT_USER_ID
        )).thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
                validator.validateAlreadyParticipated(
                        SCHEDULE_ID,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.SCHEDULE_ALREADY_PARTICIPATED);

        verify(scheduleParticipantRepository)
                .existsBySchedule_IdAndUser_Id(SCHEDULE_ID, CURRENT_USER_ID);

        // 이미 참여한 경우 공유 여부까지 확인할 필요가 없으므로 호출되지 않아야 한다.
        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 참여하지_않았지만_활성_공유가_존재하면_공유_예외가_발생한다() {
        // given
        when(scheduleParticipantRepository.existsBySchedule_IdAndUser_Id(
                SCHEDULE_ID,
                CURRENT_USER_ID
        )).thenReturn(false);

        when(scheduleShareRepository.existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                SCHEDULE_ID,
                CURRENT_USER_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
                validator.validateAlreadyParticipated(
                        SCHEDULE_ID,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.SCHEDULE_ALREADY_SHARED);

        verify(scheduleParticipantRepository)
                .existsBySchedule_IdAndUser_Id(SCHEDULE_ID, CURRENT_USER_ID);

        verify(scheduleShareRepository)
                .existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                        SCHEDULE_ID,
                        CURRENT_USER_ID,
                        ScheduleShareStatus.ACTIVE
                );
    }

    @Test
    void 참여하지도_않았고_활성_공유도_없으면_정상_통과한다() {
        // given
        when(scheduleParticipantRepository.existsBySchedule_IdAndUser_Id(
                SCHEDULE_ID,
                CURRENT_USER_ID
        )).thenReturn(false);

        when(scheduleShareRepository.existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                SCHEDULE_ID,
                CURRENT_USER_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(false);

        // when & then
        // 예외가 발생하지 않으면 정상 통과
        validator.validateAlreadyParticipated(
                SCHEDULE_ID,
                CURRENT_USER_ID
        );

        verify(scheduleParticipantRepository)
                .existsBySchedule_IdAndUser_Id(SCHEDULE_ID, CURRENT_USER_ID);

        verify(scheduleShareRepository)
                .existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                        SCHEDULE_ID,
                        CURRENT_USER_ID,
                        ScheduleShareStatus.ACTIVE
                );
    }

    @Test
    void 활성_공유가_존재하면_true를_반환한다() {
        // given
        when(scheduleShareRepository.existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                SCHEDULE_ID,
                CURRENT_USER_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(true);

        // when
        boolean result = validator.isAlreadyScheduleShared(
                SCHEDULE_ID,
                CURRENT_USER_ID
        );

        // then
        assertThat(result).isTrue();

        verify(scheduleShareRepository)
                .existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                        SCHEDULE_ID,
                        CURRENT_USER_ID,
                        ScheduleShareStatus.ACTIVE
                );
    }

    @Test
    void 활성_공유가_존재하지_않으면_false를_반환한다() {
        // given
        when(scheduleShareRepository.existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                SCHEDULE_ID,
                CURRENT_USER_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(false);

        // when
        boolean result = validator.isAlreadyScheduleShared(
                SCHEDULE_ID,
                CURRENT_USER_ID
        );

        // then
        assertThat(result).isFalse();

        verify(scheduleShareRepository)
                .existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(
                        SCHEDULE_ID,
                        CURRENT_USER_ID,
                        ScheduleShareStatus.ACTIVE
                );
    }

    @Test
    void participant가_존재하면_해당_participant를_반환한다() {
        // given
        when(scheduleParticipantRepository.findBySchedule_IdAndUser_Id(
                SCHEDULE_ID,
                CURRENT_USER_ID
        )).thenReturn(Optional.of(scheduleParticipant));

        // when
        ScheduleParticipant result = validator.validateParticipant(
                SCHEDULE_ID,
                CURRENT_USER_ID
        );

        // then
        assertThat(result).isSameAs(scheduleParticipant);

        verify(scheduleParticipantRepository)
                .findBySchedule_IdAndUser_Id(SCHEDULE_ID, CURRENT_USER_ID);
    }

    @Test
    void participant가_존재하지_않으면_접근_불가_예외가_발생한다() {
        // given
        when(scheduleParticipantRepository.findBySchedule_IdAndUser_Id(
                SCHEDULE_ID,
                CURRENT_USER_ID
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                validator.validateParticipant(
                        SCHEDULE_ID,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(scheduleParticipantRepository)
                .findBySchedule_IdAndUser_Id(SCHEDULE_ID, CURRENT_USER_ID);
    }
}