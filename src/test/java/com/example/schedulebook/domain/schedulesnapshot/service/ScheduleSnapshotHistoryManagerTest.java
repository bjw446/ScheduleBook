package com.example.schedulebook.domain.schedulesnapshot.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshot;
import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshotHistory;
import com.example.schedulebook.domain.schedulesnapshot.repository.ScheduleSnapshotHistoryRepository;
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
class ScheduleSnapshotHistoryManagerTest {

    @Mock
    private ScheduleSnapshotHistoryRepository scheduleSnapshotHistoryRepository;

    @Mock
    private ChatMessage chatMessage;

    @Mock
    private ScheduleSnapshot currentSnapshot;

    @Mock
    private ScheduleSnapshotHistory snapshotHistory;

    @Mock
    private ScheduleSnapshot historySnapshot;

    private ScheduleSnapshotHistoryManager manager;

    private static final Long CHAT_MESSAGE_ID = 1L;
    private static final Long CURRENT_VERSION = 3L;
    private static final Long REQUESTED_VERSION = 2L;

    @BeforeEach
    void setUp() {
        manager = new ScheduleSnapshotHistoryManager(
                scheduleSnapshotHistoryRepository
        );
    }

    @Test
    void chatMessage의_snapshot을_history로_저장한다() {
        // when
        manager.save(chatMessage);

        // then
        verify(scheduleSnapshotHistoryRepository)
                .save(any(ScheduleSnapshotHistory.class));
    }

    @Test
    void 요청한_version이_현재_snapshot_version과_같으면_현재_snapshot을_반환한다() {
        // given
        when(chatMessage.currentSnapshot()).thenReturn(currentSnapshot);
        when(currentSnapshot.getScheduleVersion()).thenReturn(CURRENT_VERSION);

        // when
        ScheduleSnapshot result =
                manager.findSnapshot(chatMessage, CURRENT_VERSION);

        // then
        assertThat(result).isSameAs(currentSnapshot);

        verify(chatMessage, times(2)).currentSnapshot();
        verify(currentSnapshot).getScheduleVersion();

        verifyNoInteractions(scheduleSnapshotHistoryRepository);
    }

    @Test
    void 요청한_version이_현재_version과_다르면_history에서_snapshot을_조회한다() {
        // given
        when(chatMessage.currentSnapshot()).thenReturn(currentSnapshot);
        when(currentSnapshot.getScheduleVersion()).thenReturn(CURRENT_VERSION);

        when(chatMessage.getId()).thenReturn(CHAT_MESSAGE_ID);

        when(snapshotHistory.getScheduleSnapshot())
                .thenReturn(historySnapshot);

        when(scheduleSnapshotHistoryRepository.findByChatMessageIdAndVersion(
                CHAT_MESSAGE_ID,
                REQUESTED_VERSION
        )).thenReturn(Optional.of(snapshotHistory));

        // when
        ScheduleSnapshot result =
                manager.findSnapshot(chatMessage, REQUESTED_VERSION);

        // then
        assertThat(result).isSameAs(historySnapshot);

        verify(chatMessage).currentSnapshot();
        verify(currentSnapshot).getScheduleVersion();
        verify(chatMessage).getId();

        verify(scheduleSnapshotHistoryRepository)
                .findByChatMessageIdAndVersion(
                        CHAT_MESSAGE_ID,
                        REQUESTED_VERSION
                );

        verify(snapshotHistory).getScheduleSnapshot();
    }

    @Test
    void 요청한_version의_history가_없으면_snapshot_not_found_예외가_발생한다() {
        // given
        when(chatMessage.currentSnapshot()).thenReturn(currentSnapshot);
        when(currentSnapshot.getScheduleVersion()).thenReturn(CURRENT_VERSION);

        when(chatMessage.getId()).thenReturn(CHAT_MESSAGE_ID);

        when(scheduleSnapshotHistoryRepository.findByChatMessageIdAndVersion(
                CHAT_MESSAGE_ID,
                REQUESTED_VERSION
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                manager.findSnapshot(chatMessage, REQUESTED_VERSION)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.SCHEDULE_SNAPSHOT_NOT_FOUND);

        verify(scheduleSnapshotHistoryRepository)
                .findByChatMessageIdAndVersion(
                        CHAT_MESSAGE_ID,
                        REQUESTED_VERSION
                );
    }
}