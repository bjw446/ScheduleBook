package com.example.schedulebook.domain.scheduleparticipant.service;

import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantResponse;
import com.example.schedulebook.domain.scheduleparticipant.enums.AttendanceStatus;
import com.example.schedulebook.domain.scheduleparticipant.projection.ScheduleParticipantProjection;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleParticipantReaderTest {

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    @Mock
    private ScheduleParticipantProjection firstProjection;

    @Mock
    private ScheduleParticipantProjection secondProjection;

    private ScheduleParticipantReader reader;

    private static final Long SCHEDULE_ID = 1L;
    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        reader = new ScheduleParticipantReader(scheduleParticipantRepository);
    }

    @Test
    void 현재_사용자가_참여자이면_참여상태를_true로_반환한다() {
        // given
        when(firstProjection.getUserId()).thenReturn(CURRENT_USER_ID);
        when(firstProjection.getNickname()).thenReturn("주원");
        when(firstProjection.isOwner()).thenReturn(true);
        when(firstProjection.getAttendanceStatus()).thenReturn(AttendanceStatus.ACCEPTED);

        when(secondProjection.getUserId()).thenReturn(OTHER_USER_ID);
        when(secondProjection.getNickname()).thenReturn("참여자");
        when(secondProjection.isOwner()).thenReturn(false);
        when(secondProjection.getAttendanceStatus()).thenReturn(AttendanceStatus.PENDING);

        when(scheduleParticipantRepository.findParticipants(SCHEDULE_ID))
                .thenReturn(List.of(firstProjection, secondProjection));

        // when
        ScheduleParticipantInfo result =
                reader.getParticipantInfo(SCHEDULE_ID, CURRENT_USER_ID);

        // then
        assertThat(result.participated()).isTrue();
        assertThat(result.participantCount()).isEqualTo(2);

        assertThat(result.participants())
                .hasSize(2)
                .containsExactly(
                        new ScheduleParticipantResponse(
                                CURRENT_USER_ID,
                                "주원",
                                true,
                                AttendanceStatus.ACCEPTED
                        ),
                        new ScheduleParticipantResponse(
                                OTHER_USER_ID,
                                "참여자",
                                false,
                                AttendanceStatus.PENDING
                        )
                );

        verify(scheduleParticipantRepository).findParticipants(SCHEDULE_ID);
    }

    @Test
    void 현재_사용자가_참여자가_아니면_참여상태를_false로_반환한다() {
        // given
        when(firstProjection.getUserId()).thenReturn(OTHER_USER_ID);
        when(firstProjection.getNickname()).thenReturn("참여자");
        when(firstProjection.isOwner()).thenReturn(false);
        when(firstProjection.getAttendanceStatus()).thenReturn(AttendanceStatus.ACCEPTED);

        when(secondProjection.getUserId()).thenReturn(3L);
        when(secondProjection.getNickname()).thenReturn("다른참여자");
        when(secondProjection.isOwner()).thenReturn(false);
        when(secondProjection.getAttendanceStatus()).thenReturn(AttendanceStatus.PENDING);

        when(scheduleParticipantRepository.findParticipants(SCHEDULE_ID))
                .thenReturn(List.of(firstProjection, secondProjection));

        // when
        ScheduleParticipantInfo result =
                reader.getParticipantInfo(SCHEDULE_ID, CURRENT_USER_ID);

        // then
        assertThat(result.participated()).isFalse();
        assertThat(result.participantCount()).isEqualTo(2);

        assertThat(result.participants())
                .containsExactly(
                        new ScheduleParticipantResponse(
                                OTHER_USER_ID,
                                "참여자",
                                false,
                                AttendanceStatus.ACCEPTED
                        ),
                        new ScheduleParticipantResponse(
                                3L,
                                "다른참여자",
                                false,
                                AttendanceStatus.PENDING
                        )
                );

        verify(scheduleParticipantRepository).findParticipants(SCHEDULE_ID);
    }

    @Test
    void 참여자_목록을_조회하면_참여자_수와_매핑된_참여자_목록을_반환한다() {
        // given
        when(firstProjection.getUserId()).thenReturn(CURRENT_USER_ID);
        when(firstProjection.getNickname()).thenReturn("주원");
        when(firstProjection.isOwner()).thenReturn(true);
        when(firstProjection.getAttendanceStatus()).thenReturn(AttendanceStatus.ACCEPTED);

        when(secondProjection.getUserId()).thenReturn(OTHER_USER_ID);
        when(secondProjection.getNickname()).thenReturn("참여자");
        when(secondProjection.isOwner()).thenReturn(false);
        when(secondProjection.getAttendanceStatus()).thenReturn(AttendanceStatus.PENDING);

        when(scheduleParticipantRepository.findParticipants(SCHEDULE_ID))
                .thenReturn(List.of(firstProjection, secondProjection));

        // when
        ScheduleParticipantListResponse result =
                reader.getParticipantList(SCHEDULE_ID);

        // then
        assertThat(result.participantCount()).isEqualTo(2);

        assertThat(result.participants())
                .containsExactly(
                        new ScheduleParticipantResponse(
                                CURRENT_USER_ID,
                                "주원",
                                true,
                                AttendanceStatus.ACCEPTED
                        ),
                        new ScheduleParticipantResponse(
                                OTHER_USER_ID,
                                "참여자",
                                false,
                                AttendanceStatus.PENDING
                        )
                );

        verify(scheduleParticipantRepository).findParticipants(SCHEDULE_ID);
    }

    @Test
    void 참여자가_없으면_빈_목록과_0명의_참여자_수를_반환한다() {
        // given
        when(scheduleParticipantRepository.findParticipants(SCHEDULE_ID))
                .thenReturn(List.of());

        // when
        ScheduleParticipantInfo info =
                reader.getParticipantInfo(SCHEDULE_ID, CURRENT_USER_ID);

        ScheduleParticipantListResponse list =
                reader.getParticipantList(SCHEDULE_ID);

        // then
        assertThat(info.participated()).isFalse();
        assertThat(info.participantCount()).isZero();
        assertThat(info.participants()).isEmpty();

        assertThat(list.participantCount()).isZero();
        assertThat(list.participants()).isEmpty();

        verify(scheduleParticipantRepository, org.mockito.Mockito.times(2))
                .findParticipants(SCHEDULE_ID);
    }
}