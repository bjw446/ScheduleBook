package com.example.schedulebook.domain.comment.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.notificationretry.service.NotificationRetryService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentCreatedProcessorTest {

    private static final String EVENT_ID = "event-1";
    private static final Long OUTBOX_ID = 100L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long WRITER_ID = 1L;
    private static final Long OWNER_ID = 2L;
    private static final Long PARTICIPANT_ID = 3L;
    private static final Long PARENT_COMMENT_ID = 20L;
    private static final String WRITER_NICKNAME = "작성자";

    @Mock
    private NotificationService notificationService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    @Mock
    private NotificationRetryService notificationRetryService;

    @InjectMocks
    private CommentCreatedProcessor commentCreatedProcessor;

    @Test
    void supports_댓글_생성_이벤트를_지원한다() {
        // when
        Class<CommentCreatedEvent> result =
                commentCreatedProcessor.supports();

        // then
        assertThat(result)
                .isEqualTo(CommentCreatedEvent.class);
    }

    @Test
    void process_일반_댓글이면_일정_소유자와_참여자에게_알림을_생성한다() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                null
        );

        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);

        when(scheduleRepository.findWithOwner(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getId())
                .thenReturn(OWNER_ID);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleParticipantRepository.findParticipantIds(SCHEDULE_ID))
                .thenReturn(List.of(PARTICIPANT_ID));

        // when
        commentCreatedProcessor.process(OUTBOX_ID, event);

        // then
        verify(notificationService)
                .createScheduleCommentNotification(
                        OWNER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );

        verify(notificationService)
                .createScheduleCommentNotification(
                        PARTICIPANT_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );
    }

    @Test
    void process_일반_댓글이면_댓글_작성자_본인에게는_알림을_생성하지_않는다() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                null
        );

        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);

        when(scheduleRepository.findWithOwner(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getId())
                .thenReturn(WRITER_ID);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleParticipantRepository.findParticipantIds(SCHEDULE_ID))
                .thenReturn(List.of(PARTICIPANT_ID));

        // when
        commentCreatedProcessor.process(OUTBOX_ID, event);

        // then
        verify(notificationService, never())
                .createScheduleCommentNotification(
                        WRITER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );

        verify(notificationService)
                .createScheduleCommentNotification(
                        PARTICIPANT_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );
    }

    @Test
    void process_일반_댓글이면_소유자와_참여자가_중복되어도_알림을_한번만_생성한다() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                null
        );

        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);

        when(scheduleRepository.findWithOwner(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getId())
                .thenReturn(OWNER_ID);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleParticipantRepository.findParticipantIds(SCHEDULE_ID))
                .thenReturn(List.of(OWNER_ID));

        // when
        commentCreatedProcessor.process(OUTBOX_ID, event);

        // then
        verify(notificationService)
                .createScheduleCommentNotification(
                        OWNER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );
    }

    @Test
    void process_대댓글이면_부모_댓글_작성자에게_답글_알림을_생성한다() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                PARENT_COMMENT_ID
        );

        Comment parent = mock(Comment.class);
        User parentWriter = mock(User.class);

        when(commentRepository.findWithWriter(PARENT_COMMENT_ID))
                .thenReturn(Optional.of(parent));

        when(parent.getWriter())
                .thenReturn(parentWriter);

        when(parentWriter.getId())
                .thenReturn(OWNER_ID);

        // when
        commentCreatedProcessor.process(OUTBOX_ID, event);

        // then
        verify(notificationService)
                .createCommentReplyNotification(
                        OWNER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );
    }

    @Test
    void process_대댓글_작성자와_부모_댓글_작성자가_같으면_알림을_생성하지_않는다() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                PARENT_COMMENT_ID
        );

        Comment parent = mock(Comment.class);
        User parentWriter = mock(User.class);

        when(commentRepository.findWithWriter(PARENT_COMMENT_ID))
                .thenReturn(Optional.of(parent));

        when(parent.getWriter())
                .thenReturn(parentWriter);

        when(parentWriter.getId())
                .thenReturn(WRITER_ID);

        // when
        commentCreatedProcessor.process(OUTBOX_ID, event);

        // then
        verifyNoInteractions(
                notificationService,
                notificationRetryService
        );
    }

    @Test
    void process_일반_댓글_알림_생성에_실패하면_재시도_정보를_저장한다() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                null
        );

        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);

        RuntimeException exception =
                new RuntimeException("알림 생성 실패");

        when(scheduleRepository.findWithOwner(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getId())
                .thenReturn(OWNER_ID);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleParticipantRepository.findParticipantIds(SCHEDULE_ID))
                .thenReturn(List.of());

        doThrow(exception)
                .when(notificationService)
                .createScheduleCommentNotification(
                        OWNER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );

        // when
        commentCreatedProcessor.process(OUTBOX_ID, event);

        // then
        verify(notificationRetryService)
                .save(
                        EVENT_ID,
                        OUTBOX_ID,
                        OWNER_ID,
                        NotificationType.SCHEDULE_COMMENT,
                        event,
                        exception.getMessage()
                );
    }

    @Test
    void process_대댓글_알림_생성에_실패하면_재시도_정보를_저장한다() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                PARENT_COMMENT_ID
        );

        Comment parent = mock(Comment.class);
        User parentWriter = mock(User.class);

        RuntimeException exception =
                new RuntimeException("답글 알림 생성 실패");

        when(commentRepository.findWithWriter(PARENT_COMMENT_ID))
                .thenReturn(Optional.of(parent));

        when(parent.getWriter())
                .thenReturn(parentWriter);

        when(parentWriter.getId())
                .thenReturn(OWNER_ID);

        doThrow(exception)
                .when(notificationService)
                .createCommentReplyNotification(
                        OWNER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );

        // when
        commentCreatedProcessor.process(OUTBOX_ID, event);

        // then
        verify(notificationRetryService)
                .save(
                        EVENT_ID,
                        OUTBOX_ID,
                        OWNER_ID,
                        NotificationType.COMMENT_REPLY,
                        event,
                        exception.getMessage()
                );
    }

    @Test
    void process_알림_재시도_저장에_실패하면_예외가_발생한다() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                EVENT_ID,
                SCHEDULE_ID,
                WRITER_ID,
                WRITER_NICKNAME,
                null
        );

        Schedule schedule = mock(Schedule.class);
        User owner = mock(User.class);

        RuntimeException notificationException =
                new RuntimeException("알림 생성 실패");

        RuntimeException retryException =
                new RuntimeException("Retry 저장 실패");

        when(scheduleRepository.findWithOwner(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));

        when(schedule.getUser())
                .thenReturn(owner);

        when(owner.getId())
                .thenReturn(OWNER_ID);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleParticipantRepository.findParticipantIds(SCHEDULE_ID))
                .thenReturn(List.of());

        doThrow(notificationException)
                .when(notificationService)
                .createScheduleCommentNotification(
                        OWNER_ID,
                        WRITER_NICKNAME,
                        SCHEDULE_ID
                );

        doThrow(retryException)
                .when(notificationRetryService)
                .save(
                        EVENT_ID,
                        OUTBOX_ID,
                        OWNER_ID,
                        NotificationType.SCHEDULE_COMMENT,
                        event,
                        notificationException.getMessage()
                );

        // when & then
        assertThatThrownBy(() ->
                commentCreatedProcessor.process(OUTBOX_ID, event)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.NOTIFICATION_RETRY_SAVE_FAILED)
                );
    }
}