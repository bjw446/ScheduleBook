package com.example.schedulebook.domain.scheduleshare.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatmessage.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.publisher.ChatMessagePublisher;
import com.example.schedulebook.domain.chatmessage.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chatmessage.service.ChatMessageManager;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.schedulesnapshot.service.ScheduleSnapshotManager;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.publisher.ScheduleSharePublisher;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleShareUpdateManagerTest {

    private static final Long SCHEDULE_ID = 10L;
    private static final Long USER_ID = 1L;

    private static final Long ROOM_ID_1 = 100L;
    private static final Long ROOM_ID_2 = 200L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ScheduleSnapshotManager scheduleSnapshotManager;

    @Mock
    private ChatMessageManager chatMessageManager;

    @Mock
    private ScheduleSharePublisher scheduleSharePublisher;

    @Mock
    private ChatMessagePublisher chatMessagePublisher;

    @Mock
    private ScheduleShareRepository scheduleShareRepository;

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    private ScheduleShareUpdateManager scheduleShareUpdateManager;

    @BeforeEach
    void setUp() {
        scheduleShareUpdateManager = new ScheduleShareUpdateManager(
                scheduleRepository,
                chatMessageRepository,
                scheduleSnapshotManager,
                chatMessageManager,
                scheduleSharePublisher,
                chatMessagePublisher,
                scheduleShareRepository,
                scheduleParticipantRepository
        );
    }

    // =========================================================
    // handleUpdated
    // =========================================================

    @Test
    void 일정이_수정되면_변경된_공유메시지만_발행하고_채팅방별_시스템메시지를_발행한다() {
        // given
        Schedule schedule = mock(Schedule.class);

        ChatMessage updatedMessage1 = mock(ChatMessage.class);
        ChatMessage updatedMessage2 = mock(ChatMessage.class);
        ChatMessage canceledMessage = mock(ChatMessage.class);
        ChatMessage unchangedMessage = mock(ChatMessage.class);

        ChatRoom room1 = mock(ChatRoom.class);
        ChatRoom room2 = mock(ChatRoom.class);

        PublishChatMessage systemMessage1 = mock(PublishChatMessage.class);
        PublishChatMessage systemMessage2 = mock(PublishChatMessage.class);

        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(chatMessageRepository.findAllByScheduleIdAndDeletedFalse(SCHEDULE_ID))
                .thenReturn(List.of(
                        updatedMessage1,
                        updatedMessage2,
                        canceledMessage,
                        unchangedMessage
                ));

        when(updatedMessage1.isScheduleShareCanceled())
                .thenReturn(false);

        when(updatedMessage2.isScheduleShareCanceled())
                .thenReturn(false);

        when(canceledMessage.isScheduleShareCanceled())
                .thenReturn(true);

        when(unchangedMessage.isScheduleShareCanceled())
                .thenReturn(false);

        when(scheduleSnapshotManager.updateSnapshot(
                updatedMessage1,
                schedule
        )).thenReturn(true);

        when(scheduleSnapshotManager.updateSnapshot(
                updatedMessage2,
                schedule
        )).thenReturn(true);

        when(scheduleSnapshotManager.updateSnapshot(
                unchangedMessage,
                schedule
        )).thenReturn(false);

        // 동일한 room에 두 개의 메시지가 존재
        when(updatedMessage1.getChatRoom())
                .thenReturn(room1);

        when(updatedMessage2.getChatRoom())
                .thenReturn(room1);

        when(room1.getId())
                .thenReturn(ROOM_ID_1);

        // 다른 room의 메시지
        ChatMessage anotherUpdatedMessage = mock(ChatMessage.class);

        when(anotherUpdatedMessage.isScheduleShareCanceled())
                .thenReturn(false);

        when(scheduleSnapshotManager.updateSnapshot(
                anotherUpdatedMessage,
                schedule
        )).thenReturn(true);

        when(anotherUpdatedMessage.getChatRoom())
                .thenReturn(room2);

        when(room2.getId())
                .thenReturn(ROOM_ID_2);

        when(chatMessageRepository.findAllByScheduleIdAndDeletedFalse(SCHEDULE_ID))
                .thenReturn(List.of(
                        updatedMessage1,
                        updatedMessage2,
                        canceledMessage,
                        unchangedMessage,
                        anotherUpdatedMessage
                ));

        when(chatMessageManager.createScheduleUpdatedSystemMessage(room1))
                .thenReturn(systemMessage1);

        when(chatMessageManager.createScheduleUpdatedSystemMessage(room2))
                .thenReturn(systemMessage2);

        // when
        scheduleShareUpdateManager.handleUpdated(SCHEDULE_ID);

        // then
        verify(scheduleRepository)
                .findById(SCHEDULE_ID);

        verify(scheduleSnapshotManager)
                .updateSnapshot(
                        updatedMessage1,
                        schedule
                );

        verify(scheduleSnapshotManager)
                .updateSnapshot(
                        updatedMessage2,
                        schedule
                );

        verify(scheduleSnapshotManager)
                .updateSnapshot(
                        unchangedMessage,
                        schedule
                );

        // 공유 취소 메시지는 snapshot 업데이트 대상에서 제외
        verify(scheduleSnapshotManager, never())
                .updateSnapshot(
                        canceledMessage,
                        schedule
                );

        verify(scheduleSharePublisher)
                .publishScheduleUpdated(
                        List.of(
                                updatedMessage1,
                                updatedMessage2,
                                anotherUpdatedMessage
                        )
                );

        // 같은 room의 메시지가 여러 개여도 시스템 메시지는 room당 한 번만 생성
        verify(chatMessageManager)
                .createScheduleUpdatedSystemMessage(room1);

        verify(chatMessageManager)
                .createScheduleUpdatedSystemMessage(room2);

        verify(chatMessageManager, times(1))
                .createScheduleUpdatedSystemMessage(room1);

        verify(chatMessageManager, times(1))
                .createScheduleUpdatedSystemMessage(room2);

        verify(chatMessagePublisher)
                .publishMessages(
                        List.of(
                                systemMessage1,
                                systemMessage2
                        )
                );
    }

    @Test
    void 일정이_수정되었지만_변경된_공유메시지가_없으면_시스템메시지를_생성하지_않는다() {
        // given
        Schedule schedule = mock(Schedule.class);

        ChatMessage unchangedMessage = mock(ChatMessage.class);
        ChatMessage canceledMessage = mock(ChatMessage.class);

        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(chatMessageRepository.findAllByScheduleIdAndDeletedFalse(SCHEDULE_ID))
                .thenReturn(List.of(
                        unchangedMessage,
                        canceledMessage
                ));

        when(unchangedMessage.isScheduleShareCanceled())
                .thenReturn(false);

        when(canceledMessage.isScheduleShareCanceled())
                .thenReturn(true);

        when(scheduleSnapshotManager.updateSnapshot(
                unchangedMessage,
                schedule
        )).thenReturn(false);

        // when
        scheduleShareUpdateManager.handleUpdated(SCHEDULE_ID);

        // then
        verify(scheduleSharePublisher)
                .publishScheduleUpdated(List.of());

        verify(chatMessagePublisher)
                .publishMessages(List.of());

        verifyNoInteractions(chatMessageManager);
    }

    // =========================================================
    // handleCanceled
    // =========================================================

    @Test
    void 일정이_취소되면_참가자를_삭제하고_공유메시지를_취소한_후_시스템메시지를_발행한다() {
        // given
        ScheduleParticipant participant =
                mock(ScheduleParticipant.class);

        ChatMessage message1 = mock(ChatMessage.class);
        ChatMessage message2 = mock(ChatMessage.class);
        ChatRoom room = mock(ChatRoom.class);

        PublishChatMessage systemMessage =
                mock(PublishChatMessage.class);

        when(scheduleParticipantRepository
                .findBySchedule_IdAndUser_Id(
                        SCHEDULE_ID,
                        USER_ID
                ))
                .thenReturn(Optional.of(participant));

        when(chatMessageRepository
                .findAllByScheduleIdAndDeletedFalse(SCHEDULE_ID))
                .thenReturn(List.of(
                        message1,
                        message2
                ));

        when(message1.isScheduleShareCanceled())
                .thenReturn(false);

        when(message2.isScheduleShareCanceled())
                .thenReturn(false);

        // 같은 room의 메시지
        when(message1.getChatRoom())
                .thenReturn(room);

        when(message2.getChatRoom())
                .thenReturn(room);

        when(room.getId())
                .thenReturn(ROOM_ID_1);

        when(chatMessageManager
                .createScheduleShareCanceledSystemMessage(room))
                .thenReturn(systemMessage);

        // when
        scheduleShareUpdateManager.handleCanceled(
                SCHEDULE_ID,
                USER_ID
        );

        // then
        verify(scheduleParticipantRepository)
                .findBySchedule_IdAndUser_Id(
                        SCHEDULE_ID,
                        USER_ID
                );

        verify(participant)
                .delete();

        verify(message1)
                .cancelScheduleShare();

        verify(message2)
                .cancelScheduleShare();

        verify(scheduleSharePublisher)
                .publishScheduleShareCanceled(
                        List.of(
                                message1,
                                message2
                        )
                );

        // 동일한 room이므로 시스템 메시지는 한 번만 생성
        verify(chatMessageManager)
                .createScheduleShareCanceledSystemMessage(room);

        verify(chatMessageManager, times(1))
                .createScheduleShareCanceledSystemMessage(room);

        verify(chatMessagePublisher)
                .publishMessages(
                        List.of(systemMessage)
                );
    }

    @Test
    void 일정_참가자가_아니면_일정_취소_후속처리를_수행하지_않는다() {
        // given
        when(scheduleParticipantRepository
                .findBySchedule_IdAndUser_Id(
                        SCHEDULE_ID,
                        USER_ID
                ))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                scheduleShareUpdateManager.handleCanceled(
                        SCHEDULE_ID,
                        USER_ID
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.SCHEDULE_PARTICIPANT_NOT_FOUND);

        verify(scheduleParticipantRepository)
                .findBySchedule_IdAndUser_Id(
                        SCHEDULE_ID,
                        USER_ID
                );

        verifyNoInteractions(chatMessageRepository);
        verifyNoInteractions(scheduleSharePublisher);
        verifyNoInteractions(chatMessagePublisher);
        verifyNoInteractions(chatMessageManager);
    }

    @Test
    void 이미_취소된_공유메시지는_일정_취소_처리에서_다시_취소하지_않는다() {
        // given
        ScheduleParticipant participant =
                mock(ScheduleParticipant.class);

        ChatMessage activeMessage = mock(ChatMessage.class);
        ChatMessage canceledMessage = mock(ChatMessage.class);

        ChatRoom room = mock(ChatRoom.class);

        PublishChatMessage systemMessage =
                mock(PublishChatMessage.class);

        when(scheduleParticipantRepository
                .findBySchedule_IdAndUser_Id(
                        SCHEDULE_ID,
                        USER_ID
                ))
                .thenReturn(Optional.of(participant));

        when(chatMessageRepository
                .findAllByScheduleIdAndDeletedFalse(SCHEDULE_ID))
                .thenReturn(List.of(
                        activeMessage,
                        canceledMessage
                ));

        when(activeMessage.isScheduleShareCanceled())
                .thenReturn(false);

        when(canceledMessage.isScheduleShareCanceled())
                .thenReturn(true);

        when(activeMessage.getChatRoom())
                .thenReturn(room);

        when(room.getId())
                .thenReturn(ROOM_ID_1);

        when(chatMessageManager
                .createScheduleShareCanceledSystemMessage(room))
                .thenReturn(systemMessage);

        // when
        scheduleShareUpdateManager.handleCanceled(
                SCHEDULE_ID,
                USER_ID
        );

        // then
        verify(activeMessage)
                .cancelScheduleShare();

        verify(canceledMessage, never())
                .cancelScheduleShare();

        verify(scheduleSharePublisher)
                .publishScheduleShareCanceled(
                        List.of(activeMessage)
                );

        verify(chatMessagePublisher)
                .publishMessages(
                        List.of(systemMessage)
                );
    }

    // =========================================================
    // handleDeleted
    // =========================================================

    @Test
    void 일정이_삭제되면_공유와_참가자를_삭제하고_공유메시지를_취소한_후_시스템메시지를_발행한다() {
        // given
        ScheduleShare share1 = mock(ScheduleShare.class);
        ScheduleShare share2 = mock(ScheduleShare.class);

        ScheduleParticipant activeParticipant =
                mock(ScheduleParticipant.class);

        ScheduleParticipant deletedParticipant =
                mock(ScheduleParticipant.class);

        ChatMessage message1 = mock(ChatMessage.class);
        ChatMessage message2 = mock(ChatMessage.class);
        ChatMessage canceledMessage = mock(ChatMessage.class);

        ChatRoom room = mock(ChatRoom.class);

        PublishChatMessage systemMessage =
                mock(PublishChatMessage.class);

        when(scheduleShareRepository
                .findAllBySchedule_Id(SCHEDULE_ID))
                .thenReturn(List.of(
                        share1,
                        share2
                ));

        when(scheduleParticipantRepository
                .findAllBySchedule_Id(SCHEDULE_ID))
                .thenReturn(List.of(
                        activeParticipant,
                        deletedParticipant
                ));

        when(activeParticipant.isDeleted())
                .thenReturn(false);

        when(deletedParticipant.isDeleted())
                .thenReturn(true);

        when(chatMessageRepository
                .findAllByScheduleIdAndDeletedFalse(SCHEDULE_ID))
                .thenReturn(List.of(
                        message1,
                        message2,
                        canceledMessage
                ));

        when(message1.isScheduleShareCanceled())
                .thenReturn(false);

        when(message2.isScheduleShareCanceled())
                .thenReturn(false);

        when(canceledMessage.isScheduleShareCanceled())
                .thenReturn(true);

        when(message1.getChatRoom())
                .thenReturn(room);

        when(message2.getChatRoom())
                .thenReturn(room);

        when(room.getId())
                .thenReturn(ROOM_ID_1);

        when(chatMessageManager
                .createScheduleDeletedSystemMessage(room))
                .thenReturn(systemMessage);

        // when
        scheduleShareUpdateManager.handleDeleted(SCHEDULE_ID);

        // then
        verify(scheduleShareRepository)
                .findAllBySchedule_Id(SCHEDULE_ID);

        verify(share1)
                .deletedSchedule();

        verify(share2)
                .deletedSchedule();

        verify(scheduleParticipantRepository)
                .findAllBySchedule_Id(SCHEDULE_ID);

        // 삭제되지 않은 참가자만 삭제
        verify(activeParticipant)
                .delete();

        verify(deletedParticipant, never())
                .delete();

        verify(message1)
                .cancelScheduleShare();

        verify(message2)
                .cancelScheduleShare();

        verify(canceledMessage, never())
                .cancelScheduleShare();

        verify(scheduleSharePublisher)
                .publishSharedScheduleDeleted(
                        List.of(
                                message1,
                                message2
                        )
                );

        // 동일 room의 메시지 두 개 → 시스템 메시지 한 개
        verify(chatMessageManager)
                .createScheduleDeletedSystemMessage(room);

        verify(chatMessageManager, times(1))
                .createScheduleDeletedSystemMessage(room);

        verify(chatMessagePublisher)
                .publishMessages(
                        List.of(systemMessage)
                );
    }

    @Test
    void 일정에_공유나_참가자_메시지가_없어도_삭제_후속처리를_수행한다() {
        // given
        when(scheduleShareRepository
                .findAllBySchedule_Id(SCHEDULE_ID))
                .thenReturn(List.of());

        when(scheduleParticipantRepository
                .findAllBySchedule_Id(SCHEDULE_ID))
                .thenReturn(List.of());

        when(chatMessageRepository
                .findAllByScheduleIdAndDeletedFalse(SCHEDULE_ID))
                .thenReturn(List.of());

        // when
        scheduleShareUpdateManager.handleDeleted(SCHEDULE_ID);

        // then
        verify(scheduleShareRepository)
                .findAllBySchedule_Id(SCHEDULE_ID);

        verify(scheduleParticipantRepository)
                .findAllBySchedule_Id(SCHEDULE_ID);

        verify(chatMessageRepository)
                .findAllByScheduleIdAndDeletedFalse(SCHEDULE_ID);

        verify(scheduleSharePublisher)
                .publishSharedScheduleDeleted(List.of());

        verify(chatMessagePublisher)
                .publishMessages(List.of());

        verifyNoInteractions(chatMessageManager);
    }
}