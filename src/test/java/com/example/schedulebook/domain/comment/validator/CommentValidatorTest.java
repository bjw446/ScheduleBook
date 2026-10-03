package com.example.schedulebook.domain.comment.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentValidatorTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long COMMENT_ID = 100L;
    private static final Long PARENT_COMMENT_ID = 200L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long OTHER_SCHEDULE_ID = 20L;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentValidator commentValidator;

    @Test
    void validateComment_댓글이_존재하면_댓글을_반환한다() {
        // given
        Comment comment = mock(Comment.class);

        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment));

        when(comment.isDeleted())
                .thenReturn(false);

        // when
        Comment result = commentValidator.validateComment(COMMENT_ID);

        // then
        assertThat(result)
                .isSameAs(comment);

        verify(commentRepository)
                .findById(COMMENT_ID);

        verify(comment)
                .isDeleted();
    }

    @Test
    void validateComment_댓글이_존재하지_않으면_예외가_발생한다() {
        // given
        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                commentValidator.validateComment(COMMENT_ID)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.COMMENT_NOT_FOUND)
                );

        verify(commentRepository)
                .findById(COMMENT_ID);
    }

    @Test
    void validateComment_이미_삭제된_댓글이면_예외가_발생한다() {
        // given
        Comment comment = mock(Comment.class);

        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment));

        when(comment.isDeleted())
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
                commentValidator.validateComment(COMMENT_ID)
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.COMMENT_ALREADY_DELETE)
                );

        verify(commentRepository)
                .findById(COMMENT_ID);

        verify(comment)
                .isDeleted();
    }

    @Test
    void validateParentComment_부모_댓글이_존재하고_같은_일정이면_댓글을_반환한다() {
        // given
        Comment parent = mock(Comment.class);
        Schedule schedule = mock(Schedule.class);

        when(commentRepository.findById(PARENT_COMMENT_ID))
                .thenReturn(Optional.of(parent));

        when(parent.isDeleted())
                .thenReturn(false);

        when(parent.getSchedule())
                .thenReturn(schedule);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(parent.getParent())
                .thenReturn(null);

        // when
        Comment result = commentValidator.validateParentComment(
                SCHEDULE_ID,
                PARENT_COMMENT_ID
        );

        // then
        assertThat(result)
                .isSameAs(parent);

        verify(commentRepository)
                .findById(PARENT_COMMENT_ID);

        verify(parent)
                .isDeleted();

        verify(parent)
                .getSchedule();

        verify(schedule)
                .getId();

        verify(parent)
                .getParent();
    }

    @Test
    void validateParentComment_다른_일정의_댓글이면_예외가_발생한다() {
        // given
        Comment parent = mock(Comment.class);
        Schedule schedule = mock(Schedule.class);

        when(commentRepository.findById(PARENT_COMMENT_ID))
                .thenReturn(Optional.of(parent));

        when(parent.isDeleted())
                .thenReturn(false);

        when(parent.getSchedule())
                .thenReturn(schedule);

        when(schedule.getId())
                .thenReturn(OTHER_SCHEDULE_ID);

        // when & then
        assertThatThrownBy(() ->
                commentValidator.validateParentComment(
                        SCHEDULE_ID,
                        PARENT_COMMENT_ID
                )
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.COMMENT_FORBIDDEN)
                );

        verify(commentRepository)
                .findById(PARENT_COMMENT_ID);

        verify(parent)
                .getSchedule();

        verify(schedule)
                .getId();

        verify(parent, never())
                .getParent();
    }

    @Test
    void validateParentComment_이미_대댓글인_댓글이면_예외가_발생한다() {
        // given
        Comment parent = mock(Comment.class);
        Schedule schedule = mock(Schedule.class);
        Comment grandParent = mock(Comment.class);

        when(commentRepository.findById(PARENT_COMMENT_ID))
                .thenReturn(Optional.of(parent));

        when(parent.isDeleted())
                .thenReturn(false);

        when(parent.getSchedule())
                .thenReturn(schedule);

        when(schedule.getId())
                .thenReturn(SCHEDULE_ID);

        when(parent.getParent())
                .thenReturn(grandParent);

        // when & then
        assertThatThrownBy(() ->
                commentValidator.validateParentComment(
                        SCHEDULE_ID,
                        PARENT_COMMENT_ID
                )
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.INVALID_COMMENT)
                );

        verify(commentRepository)
                .findById(PARENT_COMMENT_ID);

        verify(parent)
                .getSchedule();

        verify(schedule)
                .getId();

        verify(parent)
                .getParent();
    }

    @Test
    void validateCommentWriter_작성자가_일치하면_예외가_발생하지_않는다() {
        // given
        Comment comment = mock(Comment.class);
        User writer = mock(User.class);

        when(comment.getWriter())
                .thenReturn(writer);

        when(writer.getId())
                .thenReturn(USER_ID);

        // when & then
        assertThatCode(() ->
                commentValidator.validateCommentWriter(
                        comment,
                        USER_ID
                )
        )
                .doesNotThrowAnyException();

        verify(comment)
                .getWriter();

        verify(writer)
                .getId();
    }

    @Test
    void validateCommentWriter_작성자가_다르면_예외가_발생한다() {
        // given
        Comment comment = mock(Comment.class);
        User writer = mock(User.class);

        when(comment.getWriter())
                .thenReturn(writer);

        when(writer.getId())
                .thenReturn(USER_ID);

        // when & then
        assertThatThrownBy(() ->
                commentValidator.validateCommentWriter(
                        comment,
                        OTHER_USER_ID
                )
        )
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorEnum())
                                .isEqualTo(ErrorEnum.COMMENT_FORBIDDEN)
                );

        verify(comment)
                .getWriter();

        verify(writer)
                .getId();
    }
}