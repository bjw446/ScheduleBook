package com.example.schedulebook.domain.scheduleshare.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import com.example.schedulebook.domain.user.entity.User;
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
class ScheduleShareValidatorTest {

    private static final Long SHARE_ID = 1L;
    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private ScheduleShareRepository scheduleShareRepository;

    @Mock
    private ScheduleShare scheduleShare;

    @Mock
    private User sharedUser;

    @Mock
    private User scheduleOwner;

    @Mock
    private Schedule schedule;

    private ScheduleShareValidator scheduleShareValidator;

    @BeforeEach
    void setUp() {
        scheduleShareValidator = new ScheduleShareValidator(scheduleShareRepository);
    }

    @Test
    void 활성_상태_공유를_조회하면_공유를_반환한다() {
        // given
        when(scheduleShareRepository.findByIdWithSchedule(SHARE_ID))
                .thenReturn(Optional.of(scheduleShare));
        when(scheduleShare.getScheduleShareStatus())
                .thenReturn(ScheduleShareStatus.ACTIVE);

        // when
        ScheduleShare result = scheduleShareValidator.validateActiveScheduleShare(SHARE_ID);

        // then
        assertThat(result).isSameAs(scheduleShare);

        verify(scheduleShareRepository).findByIdWithSchedule(SHARE_ID);
        verify(scheduleShare).getScheduleShareStatus();
    }

    @Test
    void 존재하지_않는_공유를_조회하면_공유_없음_예외가_발생한다() {
        // given
        when(scheduleShareRepository.findByIdWithSchedule(SHARE_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleShareValidator.validateActiveScheduleShare(SHARE_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND);

        verify(scheduleShareRepository).findByIdWithSchedule(SHARE_ID);
        verifyNoMoreInteractions(scheduleShareRepository);
    }

    @Test
    void 활성_상태가_아닌_공유를_조회하면_잘못된_상태_예외가_발생한다() {
        // given
        when(scheduleShareRepository.findByIdWithSchedule(SHARE_ID))
                .thenReturn(Optional.of(scheduleShare));
        when(scheduleShare.getScheduleShareStatus())
                .thenReturn(ScheduleShareStatus.CANCELED);

        // when & then
        assertThatThrownBy(() -> scheduleShareValidator.validateActiveScheduleShare(SHARE_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_SCHEDULE_SHARE_STATUS);

        verify(scheduleShareRepository).findByIdWithSchedule(SHARE_ID);
        verify(scheduleShare).getScheduleShareStatus();
    }

    @Test
    void 이미_활성_상태로_공유된_경우_이미_공유됨_예외가_발생한다() {
        // given
        when(scheduleShare.getScheduleShareStatus())
                .thenReturn(ScheduleShareStatus.ACTIVE);

        // when & then
        assertThatThrownBy(() -> scheduleShareValidator.validateShareStatus(scheduleShare))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SCHEDULE_ALREADY_SHARED);

        verify(scheduleShare).getScheduleShareStatus();
    }

    @Test
    void 활성_상태가_아닌_공유는_재공유가_가능하다() {
        // given
        when(scheduleShare.getScheduleShareStatus())
                .thenReturn(ScheduleShareStatus.CANCELED);

        // when
        scheduleShareValidator.validateShareStatus(scheduleShare);

        // then
        verify(scheduleShare).getScheduleShareStatus();
    }

    @Test
    void 활성_상태_공유를_상세_조회하면_공유를_반환한다() {
        // given
        when(scheduleShareRepository.findActiveShareDetail(
                SHARE_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(Optional.of(scheduleShare));

        // when
        ScheduleShare result = scheduleShareValidator.validateScheduleShare(SHARE_ID);

        // then
        assertThat(result).isSameAs(scheduleShare);

        verify(scheduleShareRepository).findActiveShareDetail(
                SHARE_ID,
                ScheduleShareStatus.ACTIVE
        );
    }

    @Test
    void 활성_공유가_존재하지_않으면_공유_없음_예외가_발생한다() {
        // given
        when(scheduleShareRepository.findActiveShareDetail(
                SHARE_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleShareValidator.validateScheduleShare(SHARE_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND);

        verify(scheduleShareRepository).findActiveShareDetail(
                SHARE_ID,
                ScheduleShareStatus.ACTIVE
        );
    }

    @Test
    void 공유받은_사용자_본인이면_정상_통과한다() {
        // given
        when(scheduleShare.getSharedUser())
                .thenReturn(sharedUser);
        when(sharedUser.getId())
                .thenReturn(CURRENT_USER_ID);

        // when
        scheduleShareValidator.validateSharedUser(
                scheduleShare,
                CURRENT_USER_ID
        );

        // then
        verify(scheduleShare).getSharedUser();
        verify(sharedUser).getId();
    }

    @Test
    void 공유받은_사용자가_아니면_권한_예외가_발생한다() {
        // given
        when(scheduleShare.getSharedUser())
                .thenReturn(sharedUser);
        when(sharedUser.getId())
                .thenReturn(OTHER_USER_ID);

        // when & then
        assertThatThrownBy(() ->
                scheduleShareValidator.validateSharedUser(
                        scheduleShare,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(scheduleShare).getSharedUser();
        verify(sharedUser).getId();
    }

    @Test
    void 일정_소유자_본인이면_정상_통과한다() {
        // given
        when(scheduleShare.getSchedule())
                .thenReturn(schedule);
        when(schedule.getUser())
                .thenReturn(scheduleOwner);
        when(scheduleOwner.getId())
                .thenReturn(CURRENT_USER_ID);

        // when
        scheduleShareValidator.validateShareOwner(
                scheduleShare,
                CURRENT_USER_ID
        );

        // then
        verify(scheduleShare).getSchedule();
        verify(schedule).getUser();
        verify(scheduleOwner).getId();
    }

    @Test
    void 일정_소유자가_아니면_권한_예외가_발생한다() {
        // given
        when(scheduleShare.getSchedule())
                .thenReturn(schedule);
        when(schedule.getUser())
                .thenReturn(scheduleOwner);
        when(scheduleOwner.getId())
                .thenReturn(OTHER_USER_ID);

        // when & then
        assertThatThrownBy(() ->
                scheduleShareValidator.validateShareOwner(
                        scheduleShare,
                        CURRENT_USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(scheduleShare).getSchedule();
        verify(schedule).getUser();
        verify(scheduleOwner).getId();
    }

    @Test
    void 소유한_활성_공유를_조회하면_공유를_반환한다() {
        // given
        when(scheduleShareRepository.findOwnedShareDetail(
                SHARE_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(Optional.of(scheduleShare));

        // when
        ScheduleShare result = scheduleShareValidator.validateOwnedShare(SHARE_ID);

        // then
        assertThat(result).isSameAs(scheduleShare);

        verify(scheduleShareRepository).findOwnedShareDetail(
                SHARE_ID,
                ScheduleShareStatus.ACTIVE
        );
    }

    @Test
    void 소유한_활성_공유가_존재하지_않으면_공유_없음_예외가_발생한다() {
        // given
        when(scheduleShareRepository.findOwnedShareDetail(
                SHARE_ID,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleShareValidator.validateOwnedShare(SHARE_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND);

        verify(scheduleShareRepository).findOwnedShareDetail(
                SHARE_ID,
                ScheduleShareStatus.ACTIVE
        );
    }
}