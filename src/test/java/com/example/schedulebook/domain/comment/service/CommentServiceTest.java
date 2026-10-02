package com.example.schedulebook.domain.comment.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.dto.request.CreateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.request.UpdateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.dto.response.ScheduleCommentListResponse;
import com.example.schedulebook.domain.comment.dto.response.ScheduleCommentResponse;
import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import com.example.schedulebook.domain.comment.validator.CommentValidator;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.schedule.validator.ScheduleValidator;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long OTHER_SCHEDULE_ID = 20L;
    private static final Long COMMENT_ID = 100L;
    private static final Long PARENT_COMMENT_ID = 50L;

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleValidator scheduleValidator;

    @Mock
    private UserValidator userValidator;

    @Mock
    private CommentValidator commentValidator;

    @Mock
    private OutboxService outboxService;

    @Test
    void createComment_일반_댓글을_생성하면_댓글을_저장하고_댓글_수를_증가시키며_Outbox_이벤트를_저장한다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);

        CreateScheduleCommentRequest request =
                new CreateScheduleCommentRequest(
                        "첫 번째 댓글입니다.",
                        null
                );

        when(user.getId())
                .thenReturn(USER_ID);

        when(user.getDisplayNickname())
                .thenReturn("작성자");

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(scheduleRepository.findCommentCount(SCHEDULE_ID))
                .thenReturn(1);

        // when
        commentService.createComment(
                USER_ID,
                SCHEDULE_ID,
                request
        );

        // then
        ArgumentCaptor<Comment> commentCaptor =
                ArgumentCaptor.forClass(Comment.class);

        verify(commentRepository)
                .save(commentCaptor.capture());

        Comment savedComment = commentCaptor.getValue();

        assertThat(savedComment.getSchedule())
                .isSameAs(schedule);
        assertThat(savedComment.getWriter())
                .isSameAs(user);
        assertThat(savedComment.getParent())
                .isNull();
        assertThat(savedComment.getContent())
                .isEqualTo("첫 번째 댓글입니다.");
        assertThat(savedComment.isEdited())
                .isFalse();

        verify(scheduleRepository)
                .increaseCommentCount(SCHEDULE_ID);

        verify(scheduleRepository)
                .findCommentCount(SCHEDULE_ID);

        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<OutboxEventType> eventTypeCaptor =
                ArgumentCaptor.forClass(OutboxEventType.class);

        ArgumentCaptor<Object> payloadCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(outboxService, org.mockito.Mockito.times(2))
                .save(
                        eventIdCaptor.capture(),
                        eq(OutboxAggregateType.COMMENT),
                        anyString(),
                        eventTypeCaptor.capture(),
                        payloadCaptor.capture()
                );

        assertThat(eventIdCaptor.getAllValues())
                .allMatch(eventId -> eventId != null && !eventId.isBlank());

        assertThat(eventTypeCaptor.getAllValues())
                .containsExactlyInAnyOrder(
                        OutboxEventType.COMMENT_EVENT,
                        OutboxEventType.COMMENT_CREATED
                );

        assertThat(payloadCaptor.getAllValues())
                .anyMatch(CommentEventResponse.class::isInstance)
                .anyMatch(CommentCreatedEvent.class::isInstance);
    }

    @Test
    void createComment_대댓글을_생성하면_부모_댓글을_검증하고_parent로_설정한다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);
        Comment parentComment = mock(Comment.class);

        CreateScheduleCommentRequest request =
                new CreateScheduleCommentRequest(
                        "대댓글입니다.",
                        PARENT_COMMENT_ID
                );

        when(user.getId())
                .thenReturn(USER_ID);

        when(user.getDisplayNickname())
                .thenReturn("작성자");

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(parentComment.getId())
                .thenReturn(PARENT_COMMENT_ID);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(commentValidator.validateParentComment(
                SCHEDULE_ID,
                PARENT_COMMENT_ID
        )).thenReturn(parentComment);

        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(scheduleRepository.findCommentCount(SCHEDULE_ID))
                .thenReturn(2);

        // when
        commentService.createComment(
                USER_ID,
                SCHEDULE_ID,
                request
        );

        // then
        verify(commentValidator)
                .validateParentComment(
                        SCHEDULE_ID,
                        PARENT_COMMENT_ID
                );

        ArgumentCaptor<Comment> commentCaptor =
                ArgumentCaptor.forClass(Comment.class);

        verify(commentRepository)
                .save(commentCaptor.capture());

        Comment savedComment = commentCaptor.getValue();

        assertThat(savedComment.getSchedule())
                .isSameAs(schedule);
        assertThat(savedComment.getWriter())
                .isSameAs(user);
        assertThat(savedComment.getParent())
                .isSameAs(parentComment);
        assertThat(savedComment.getContent())
                .isEqualTo("대댓글입니다.");

        verify(outboxService, org.mockito.Mockito.times(2))
                .save(
                        anyString(),
                        eq(OutboxAggregateType.COMMENT),
                        anyString(),
                        any(OutboxEventType.class),
                        any()
                );

        verify(scheduleRepository)
                .increaseCommentCount(SCHEDULE_ID);

        verify(scheduleRepository)
                .findCommentCount(SCHEDULE_ID);
    }

    @Test
    void createComment_다른_일정의_댓글을_parent로_지정하면_댓글을_생성하지_않는다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);

        CreateScheduleCommentRequest request =
                new CreateScheduleCommentRequest(
                        "잘못된 대댓글입니다.",
                        PARENT_COMMENT_ID
                );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(commentValidator.validateParentComment(
                SCHEDULE_ID,
                PARENT_COMMENT_ID
        )).thenThrow(
                new BaseException(ErrorEnum.COMMENT_FORBIDDEN)
        );

        // when & then
        assertThatThrownBy(() ->
                commentService.createComment(
                        USER_ID,
                        SCHEDULE_ID,
                        request
                )
        )
                .isInstanceOf(BaseException.class);

        verify(commentValidator)
                .validateParentComment(
                        SCHEDULE_ID,
                        PARENT_COMMENT_ID
                );

        verify(commentRepository, never())
                .save(any(Comment.class));

        verify(scheduleRepository, never())
                .increaseCommentCount(anyLong());

        verify(outboxService, never())
                .save(
                        anyString(),
                        any(OutboxAggregateType.class),
                        anyString(),
                        any(OutboxEventType.class),
                        any()
                );
    }

    @Test
    void createComment_접근할_수_없는_일정이면_댓글을_생성하지_않는다() {
        // given
        CreateScheduleCommentRequest request =
                new CreateScheduleCommentRequest(
                        "댓글입니다.",
                        null
                );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(mock(User.class));

        when(scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenThrow(
                new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN)
        );

        // when & then
        assertThatThrownBy(() ->
                commentService.createComment(
                        USER_ID,
                        SCHEDULE_ID,
                        request
                )
        )
                .isInstanceOf(BaseException.class);

        verify(commentRepository, never())
                .save(any(Comment.class));

        verify(scheduleRepository, never())
                .increaseCommentCount(anyLong());

        verify(outboxService, never())
                .save(
                        anyString(),
                        any(OutboxAggregateType.class),
                        anyString(),
                        any(OutboxEventType.class),
                        any()
                );
    }

    @Test
    void findAllComment_부모_댓글과_대댓글을_트리_구조로_반환한다() {
        // given
        User user = mock(User.class);
        User otherUser = mock(User.class);

        Schedule schedule = mock(Schedule.class);

        Comment parent = mock(Comment.class);
        Comment reply = mock(Comment.class);

        when(user.getId())
                .thenReturn(USER_ID);
        when(user.getDisplayNickname())
                .thenReturn("작성자");
        when(user.getDeletedAt())
                .thenReturn(null);

        when(otherUser.getId())
                .thenReturn(OTHER_USER_ID);
        when(otherUser.getDisplayNickname())
                .thenReturn("다른 사용자");
        when(otherUser.getDeletedAt())
                .thenReturn(null);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);
        when(schedule.getCommentCount())
                .thenReturn(3);

        when(parent.getId())
                .thenReturn(COMMENT_ID);
        when(parent.getParent())
                .thenReturn(null);
        when(parent.getWriter())
                .thenReturn(user);
        when(parent.getContent())
                .thenReturn("부모 댓글");
        when(parent.isEdited())
                .thenReturn(false);
        when(parent.isDeleted())
                .thenReturn(false);
        when(parent.getCreatedAt())
                .thenReturn(
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                10,
                                0
                        )
                );

        when(reply.getId())
                .thenReturn(101L);
        when(reply.getParent())
                .thenReturn(parent);
        when(reply.getWriter())
                .thenReturn(otherUser);
        when(reply.getContent())
                .thenReturn("대댓글");
        when(reply.isEdited())
                .thenReturn(false);
        when(reply.isDeleted())
                .thenReturn(false);
        when(reply.getCreatedAt())
                .thenReturn(
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                10,
                                5
                        )
                );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(commentRepository.findParentComments(SCHEDULE_ID))
                .thenReturn(List.of(parent));

        when(commentRepository.findReplies(List.of(COMMENT_ID)))
                .thenReturn(List.of(reply));

        // when
        ScheduleCommentListResponse result =
                commentService.findAllComment(
                        USER_ID,
                        SCHEDULE_ID
                );

        // then
        assertThat(result.scheduleId())
                .isEqualTo(SCHEDULE_ID);

        assertThat(result.commentCount())
                .isEqualTo(3);

        assertThat(result.comments())
                .hasSize(1);

        ScheduleCommentResponse parentResponse =
                result.comments().get(0);

        assertThat(parentResponse.id())
                .isEqualTo(COMMENT_ID);

        assertThat(parentResponse.parentId())
                .isNull();

        assertThat(parentResponse.writerId())
                .isEqualTo(USER_ID);

        assertThat(parentResponse.writerNickname())
                .isEqualTo("작성자");

        assertThat(parentResponse.content())
                .isEqualTo("부모 댓글");

        assertThat(parentResponse.mine())
                .isTrue();

        assertThat(parentResponse.replies())
                .hasSize(1);

        ScheduleCommentResponse replyResponse =
                parentResponse.replies().get(0);

        assertThat(replyResponse.id())
                .isEqualTo(101L);

        assertThat(replyResponse.parentId())
                .isEqualTo(COMMENT_ID);

        assertThat(replyResponse.writerId())
                .isEqualTo(OTHER_USER_ID);

        assertThat(replyResponse.writerNickname())
                .isEqualTo("다른 사용자");

        assertThat(replyResponse.content())
                .isEqualTo("대댓글");

        assertThat(replyResponse.mine())
                .isFalse();

        verify(commentRepository)
                .findParentComments(SCHEDULE_ID);

        verify(commentRepository)
                .findReplies(List.of(COMMENT_ID));
    }

    @Test
    void findAllComment_댓글이_없으면_빈_목록을_반환한다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getCommentCount())
                .thenReturn(0);

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(commentRepository.findParentComments(SCHEDULE_ID))
                .thenReturn(List.of());

        when(commentRepository.findReplies(List.of()))
                .thenReturn(List.of());

        // when
        ScheduleCommentListResponse result =
                commentService.findAllComment(
                        USER_ID,
                        SCHEDULE_ID
                );

        // then
        assertThat(result.scheduleId())
                .isEqualTo(SCHEDULE_ID);

        assertThat(result.commentCount())
                .isZero();

        assertThat(result.comments())
                .isEmpty();

        verify(commentRepository)
                .findParentComments(SCHEDULE_ID);

        verify(commentRepository)
                .findReplies(List.of());
    }

    @Test
    void findAllComment_현재_사용자의_댓글이면_mine이_true로_매핑된다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);
        Comment comment = mock(Comment.class);

        when(user.getId())
                .thenReturn(USER_ID);

        when(user.getDisplayNickname())
                .thenReturn("작성자");

        when(user.getDeletedAt())
                .thenReturn(null);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getCommentCount())
                .thenReturn(1);

        when(comment.getId())
                .thenReturn(COMMENT_ID);

        when(comment.getParent())
                .thenReturn(null);

        when(comment.getWriter())
                .thenReturn(user);

        when(comment.getContent())
                .thenReturn("내 댓글");

        when(comment.isEdited())
                .thenReturn(false);

        when(comment.isDeleted())
                .thenReturn(false);

        when(comment.getCreatedAt())
                .thenReturn(
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                10,
                                0
                        )
                );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(user);

        when(scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(commentRepository.findParentComments(SCHEDULE_ID))
                .thenReturn(List.of(comment));

        when(commentRepository.findReplies(List.of(COMMENT_ID)))
                .thenReturn(List.of());

        // when
        ScheduleCommentListResponse result =
                commentService.findAllComment(
                        USER_ID,
                        SCHEDULE_ID
                );

        // then
        assertThat(result.comments())
                .hasSize(1);

        assertThat(result.comments().get(0).mine())
                .isTrue();
    }

    @Test
    void findAllComment_삭제된_작성자의_댓글이면_mine이_false로_매핑된다() {
        // given
        User deletedUser = mock(User.class);
        User currentUser = mock(User.class);
        Schedule schedule = mock(Schedule.class);
        Comment comment = mock(Comment.class);

        when(deletedUser.getId())
                .thenReturn(USER_ID);

        when(deletedUser.getDisplayNickname())
                .thenReturn("탈퇴한 사용자");

        when(deletedUser.getDeletedAt())
                .thenReturn(
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                9,
                                0
                        )
                );

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(schedule.getCommentCount())
                .thenReturn(1);

        when(comment.getId())
                .thenReturn(COMMENT_ID);

        when(comment.getParent())
                .thenReturn(null);

        when(comment.getWriter())
                .thenReturn(deletedUser);

        when(comment.getContent())
                .thenReturn("탈퇴한 사용자의 댓글");

        when(comment.isEdited())
                .thenReturn(false);

        when(comment.isDeleted())
                .thenReturn(false);

        when(comment.getCreatedAt())
                .thenReturn(
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                10,
                                0
                        )
                );

        when(userValidator.validateActiveUser(USER_ID))
                .thenReturn(currentUser);

        when(scheduleValidator.validateAccessibleSchedule(
                SCHEDULE_ID,
                USER_ID
        )).thenReturn(schedule);

        when(commentRepository.findParentComments(SCHEDULE_ID))
                .thenReturn(List.of(comment));

        when(commentRepository.findReplies(List.of(COMMENT_ID)))
                .thenReturn(List.of());

        // when
        ScheduleCommentListResponse result =
                commentService.findAllComment(
                        USER_ID,
                        SCHEDULE_ID
                );

        // then
        assertThat(result.comments())
                .hasSize(1);

        assertThat(result.comments().get(0).mine())
                .isFalse();
    }

    @Test
    void updateComment_작성자가_수정하면_댓글_내용과_수정_상태가_변경되고_Outbox_이벤트를_저장한다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);

        Comment comment =
                Comment.create(
                        schedule,
                        user,
                        "기존 댓글"
                );

        UpdateScheduleCommentRequest request =
                new UpdateScheduleCommentRequest(
                        "수정된 댓글"
                );

        when(user.getId())
                .thenReturn(USER_ID);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(commentValidator.validateComment(COMMENT_ID))
                .thenReturn(comment);

        // when
        commentService.updateComment(
                USER_ID,
                COMMENT_ID,
                request
        );

        // then
        assertThat(comment.getContent())
                .isEqualTo("수정된 댓글");

        assertThat(comment.isEdited())
                .isTrue();

        verify(commentValidator)
                .validateComment(COMMENT_ID);

        verify(commentValidator)
                .validateCommentWriter(
                        comment,
                        USER_ID
                );

        verify(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.COMMENT),
                        eq(String.valueOf(COMMENT_ID)),
                        eq(OutboxEventType.COMMENT_EVENT),
                        any(CommentEventResponse.class)
                );
    }

    @Test
    void updateComment_작성자가_아니면_댓글을_수정하지_않는다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);

        Comment comment =
                Comment.create(
                        schedule,
                        user,
                        "기존 댓글"
                );

        UpdateScheduleCommentRequest request =
                new UpdateScheduleCommentRequest(
                        "수정된 댓글"
                );

        when(commentValidator.validateComment(COMMENT_ID))
                .thenReturn(comment);

        doThrow(
                new BaseException(ErrorEnum.COMMENT_FORBIDDEN)
        ).when(commentValidator)
                .validateCommentWriter(
                        comment,
                        OTHER_USER_ID
                );

        // when & then
        assertThatThrownBy(() ->
                commentService.updateComment(
                        OTHER_USER_ID,
                        COMMENT_ID,
                        request
                )
        )
                .isInstanceOf(BaseException.class);

        assertThat(comment.getContent())
                .isEqualTo("기존 댓글");

        assertThat(comment.isEdited())
                .isFalse();

        verify(outboxService, never())
                .save(
                        anyString(),
                        any(OutboxAggregateType.class),
                        anyString(),
                        any(OutboxEventType.class),
                        any()
                );
    }

    @Test
    void deleteComment_작성자가_삭제하면_soft_delete하고_댓글_수를_감소시키며_Outbox_이벤트를_저장한다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);

        Comment comment =
                Comment.create(
                        schedule,
                        user,
                        "삭제할 댓글"
                );

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(commentValidator.validateComment(COMMENT_ID))
                .thenReturn(comment);

        when(scheduleRepository.findCommentCount(SCHEDULE_ID))
                .thenReturn(2);

        // when
        commentService.deleteComment(
                USER_ID,
                COMMENT_ID
        );

        // then
        assertThat(comment.isDeleted())
                .isTrue();

        assertThat(comment.getContent())
                .isEqualTo("삭제된 댓글입니다.");

        verify(commentValidator)
                .validateCommentWriter(
                        comment,
                        USER_ID
                );

        verify(scheduleRepository)
                .decreaseCommentCount(
                        SCHEDULE_ID,
                        1
                );

        verify(scheduleRepository)
                .findCommentCount(SCHEDULE_ID);

        verify(outboxService)
                .save(
                        anyString(),
                        eq(OutboxAggregateType.COMMENT),
                        eq(String.valueOf(COMMENT_ID)),
                        eq(OutboxEventType.COMMENT_EVENT),
                        any(CommentEventResponse.class)
                );
    }

    @Test
    void deleteComment_작성자가_아니면_삭제하지_않고_댓글_수도_변경하지_않는다() {
        // given
        User user = mock(User.class);
        Schedule schedule = mock(Schedule.class);

        Comment comment =
                Comment.create(
                        schedule,
                        user,
                        "삭제할 댓글"
                );

        when(commentValidator.validateComment(COMMENT_ID))
                .thenReturn(comment);

        doThrow(
                new BaseException(ErrorEnum.COMMENT_FORBIDDEN)
        ).when(commentValidator)
                .validateCommentWriter(
                        comment,
                        OTHER_USER_ID
                );

        // when & then
        assertThatThrownBy(() ->
                commentService.deleteComment(
                        OTHER_USER_ID,
                        COMMENT_ID
                )
        )
                .isInstanceOf(BaseException.class);

        assertThat(comment.isDeleted())
                .isFalse();

        assertThat(comment.getContent())
                .isEqualTo("삭제할 댓글");

        verify(scheduleRepository, never())
                .decreaseCommentCount(
                        anyLong(),
                        org.mockito.ArgumentMatchers.anyInt()
                );

        verify(outboxService, never())
                .save(
                        anyString(),
                        any(OutboxAggregateType.class),
                        anyString(),
                        any(OutboxEventType.class),
                        any()
                );
    }

    @Test
    void deleteComment_이미_삭제된_댓글이면_삭제하지_않는다() {
        // given
        when(commentValidator.validateComment(COMMENT_ID))
                .thenThrow(
                        new BaseException(
                                ErrorEnum.COMMENT_ALREADY_DELETE
                        )
                );

        // when & then
        assertThatThrownBy(() ->
                commentService.deleteComment(
                        USER_ID,
                        COMMENT_ID
                )
        )
                .isInstanceOf(BaseException.class);

        verify(scheduleRepository, never())
                .decreaseCommentCount(
                        anyLong(),
                        org.mockito.ArgumentMatchers.anyInt()
                );

        verify(outboxService, never())
                .save(
                        anyString(),
                        any(OutboxAggregateType.class),
                        anyString(),
                        any(OutboxEventType.class),
                        any()
                );
    }

    @Test
    void removeAllComments_작성자의_삭제되지_않은_댓글을_모두_soft_delete하고_일정별로_댓글_수를_감소시킨다() {
        // given
        User user = mock(User.class);
        Schedule scheduleA = mock(Schedule.class);
        Schedule scheduleB = mock(Schedule.class);

        Comment scheduleAComment1 =
                Comment.create(
                        scheduleA,
                        user,
                        "댓글 1"
                );

        Comment scheduleAComment2 =
                Comment.create(
                        scheduleA,
                        user,
                        "댓글 2"
                );

        Comment scheduleBComment =
                Comment.create(
                        scheduleB,
                        user,
                        "댓글 3"
                );

        Comment alreadyDeletedComment =
                Comment.create(
                        scheduleA,
                        user,
                        "이미 삭제된 댓글"
                );

        alreadyDeletedComment.deleteComment();

        when(scheduleA.getId())
                .thenReturn(SCHEDULE_ID);

        when(scheduleB.getId())
                .thenReturn(OTHER_SCHEDULE_ID);

        when(commentRepository.findAllByWriterId(USER_ID))
                .thenReturn(
                        List.of(
                                scheduleAComment1,
                                scheduleAComment2,
                                scheduleBComment,
                                alreadyDeletedComment
                        )
                );

        // when
        commentService.removeAllComments(USER_ID);

        // then
        assertThat(scheduleAComment1.isDeleted())
                .isTrue();

        assertThat(scheduleAComment2.isDeleted())
                .isTrue();

        assertThat(scheduleBComment.isDeleted())
                .isTrue();

        assertThat(alreadyDeletedComment.isDeleted())
                .isTrue();

        verify(scheduleRepository)
                .decreaseCommentCount(
                        SCHEDULE_ID,
                        2
                );

        verify(scheduleRepository)
                .decreaseCommentCount(
                        OTHER_SCHEDULE_ID,
                        1
                );
    }

    @Test
    void removeAllComments_댓글이_없으면_댓글_수_감소를_호출하지_않는다() {
        // given
        when(commentRepository.findAllByWriterId(USER_ID))
                .thenReturn(List.of());

        // when
        commentService.removeAllComments(USER_ID);

        // then
        verify(commentRepository)
                .findAllByWriterId(USER_ID);

        verify(scheduleRepository, never())
                .decreaseCommentCount(
                        anyLong(),
                        org.mockito.ArgumentMatchers.anyInt()
                );
    }

    @Test
    void removeAllComments_이미_삭제된_댓글만_있으면_댓글_수를_감소시키지_않는다() {
        // given
        Comment deletedComment =
                Comment.create(
                        mock(Schedule.class),
                        mock(User.class),
                        "이미 삭제된 댓글"
                );

        deletedComment.deleteComment();

        when(commentRepository.findAllByWriterId(USER_ID))
                .thenReturn(List.of(deletedComment));

        // when
        commentService.removeAllComments(USER_ID);

        // then
        verify(scheduleRepository, never())
                .decreaseCommentCount(
                        anyLong(),
                        org.mockito.ArgumentMatchers.anyInt()
                );
    }
}