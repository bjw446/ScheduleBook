package com.example.schedulebook.domain.schedulesnapshot.service;

import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleSnapshotManagerTest {

    @Mock
    private ScheduleSnapshotHistoryManager scheduleSnapshotHistoryManager;

    @Mock
    private ChatMessage chatMessage;

    @Mock
    private Schedule schedule;

    private ScheduleSnapshotManager manager;

    @BeforeEach
    void setUp() {
        manager = new ScheduleSnapshotManager(
                scheduleSnapshotHistoryManager
        );
    }

    @Test
    void 일정이_변경되었으면_기존_snapshot을_저장하고_새로운_일정으로_snapshot을_갱신한다() {
        // given
        when(chatMessage.needSnapshotUpdate(schedule))
                .thenReturn(true);

        // when
        boolean result = manager.updateSnapshot(chatMessage, schedule);

        // then
        assertThat(result).isTrue();

        InOrder inOrder = inOrder(
                scheduleSnapshotHistoryManager,
                chatMessage
        );

        inOrder.verify(scheduleSnapshotHistoryManager)
                .save(chatMessage);

        inOrder.verify(chatMessage)
                .updateScheduleSnapshot(schedule);
    }

    @Test
    void 일정이_변경되지_않았으면_snapshot을_갱신하지_않는다() {
        // given
        when(chatMessage.needSnapshotUpdate(schedule))
                .thenReturn(false);

        // when
        boolean result = manager.updateSnapshot(chatMessage, schedule);

        // then
        assertThat(result).isFalse();

        verify(chatMessage).needSnapshotUpdate(schedule);
        verifyNoInteractions(scheduleSnapshotHistoryManager);

        verify(chatMessage, never())
                .updateScheduleSnapshot(any());
    }
}