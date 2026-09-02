package com.example.schedulebook.common.websocket.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleSubscriptionValidatorTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleShareRepository scheduleShareRepository;

    @Mock
    private ScheduleShare scheduleShare;

    private ScheduleSubscriptionValidator validator;

    private final Long userId = 1L;
    private final Long scheduleId = 100L;

    @BeforeEach
    void setUp() {
        validator = new ScheduleSubscriptionValidator(
                scheduleRepository,
                scheduleShareRepository
        );
    }

    @Test
    void 일정_destination을_지원한다() {
        // when
        boolean result = validator.supports("/topic/schedule/100");

        // then
        assertTrue(result);
    }

    @Test
    void 일정이_아닌_destination은_지원하지_않는다() {
        // when
        boolean result = validator.supports("/topic/chat/100");

        // then
        assertFalse(result);
    }

    @Test
    void 일정_소유자는_구독을_허용한다() {
        // given
        when(scheduleRepository.existsByIdAndUser_Id(scheduleId, userId))
                .thenReturn(true);

        // when & then
        assertDoesNotThrow(
                () -> validator.validate(userId, "/topic/schedule/100")
        );

        verify(scheduleRepository)
                .existsByIdAndUser_Id(scheduleId, userId);
        verifyNoInteractions(scheduleShareRepository);
    }

    @Test
    void 일정_공유_사용자는_구독을_허용한다() {
        // given
        when(scheduleRepository.existsByIdAndUser_Id(scheduleId, userId))
                .thenReturn(false);

        when(scheduleShareRepository.findActiveRelation(
                scheduleId,
                userId,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(Optional.of(scheduleShare));

        // when & then
        assertDoesNotThrow(
                () -> validator.validate(userId, "/topic/schedule/100")
        );

        verify(scheduleRepository)
                .existsByIdAndUser_Id(scheduleId, userId);
        verify(scheduleShareRepository)
                .findActiveRelation(
                        scheduleId,
                        userId,
                        ScheduleShareStatus.ACTIVE
                );
    }

    @Test
    void 일정에_접근_권한이_없는_사용자는_SCHEDULE_FORBIDDEN을_던진다() {
        // given
        when(scheduleRepository.existsByIdAndUser_Id(scheduleId, userId))
                .thenReturn(false);

        when(scheduleShareRepository.findActiveRelation(
                scheduleId,
                userId,
                ScheduleShareStatus.ACTIVE
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> validator.validate(userId, "/topic/schedule/100")
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SCHEDULE_FORBIDDEN);

        verify(scheduleRepository)
                .existsByIdAndUser_Id(scheduleId, userId);
        verify(scheduleShareRepository)
                .findActiveRelation(
                        scheduleId,
                        userId,
                        ScheduleShareStatus.ACTIVE
                );
    }

    @Test
    void 잘못된_일정_destination은_INVALID_INPUT을_던진다() {
        // given
        String invalidDestination = "/topic/schedule/not-a-number";

        // when & then
        assertThatThrownBy(
                () -> validator.validate(userId, invalidDestination)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        verifyNoInteractions(scheduleRepository, scheduleShareRepository);
    }

    @Test
    void destination에서_scheduleId를_정확하게_파싱한다() {
        // given
        when(scheduleRepository.existsByIdAndUser_Id(scheduleId, userId))
                .thenReturn(true);

        // when
        validator.validate(userId, "/topic/schedule/100/messages");

        // then
        verify(scheduleRepository)
                .existsByIdAndUser_Id(scheduleId, userId);
        verifyNoInteractions(scheduleShareRepository);
    }
}