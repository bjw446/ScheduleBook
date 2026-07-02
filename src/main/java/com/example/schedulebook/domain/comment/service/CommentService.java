package com.example.schedulebook.domain.comment.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.dto.request.CreateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.request.UpdateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.dto.response.ScheduleCommentListResponse;
import com.example.schedulebook.domain.comment.dto.response.ScheduleCommentResponse;
import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.enums.CommentEventType;
import com.example.schedulebook.domain.comment.event.CommentCreatedEvent;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import com.example.schedulebook.domain.comment.event.CommentPublisher;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final CommentPublisher commentPublisher;
    private final ApplicationEventPublisher eventPublisher;

    public void createComment(Long currentUserId, Long scheduleId, CreateScheduleCommentRequest request) {
        User user = validateUser(currentUserId);

        Schedule schedule = validateSchedule(scheduleId);

        validateScheduleAccessible(schedule, currentUserId);

        Comment parent = null;

        if (request.parentCommentId() != null) {
            parent = validateParentComment(scheduleId, request.parentCommentId());
        }

        Comment comment;

        if (parent == null) {
            comment = Comment.create(schedule, user, request.content());
        } else {
            comment = Comment.reply(schedule, user, parent, request.content());
        }

        Comment savedComment = commentRepository.save(comment);

        scheduleRepository.increaseCommentCount(scheduleId);

        int commentCount = getCurrentCommentCount(scheduleId);

        publishCommentEvent(savedComment, CommentEventType.CREATED, commentCount);

        CommentCreatedEvent createdEvent = new CommentCreatedEvent(
                scheduleId,
                user.getId(),
                user.getNickname(),
                parent == null ? null : parent.getId()
        );

        eventPublisher.publishEvent(createdEvent);
    }

    @Transactional(readOnly = true)
    public ScheduleCommentListResponse findAllComment(Long currentUserId, Long scheduleId) {
        validateUser(currentUserId);

        Schedule schedule = validateSchedule(scheduleId);

        validateScheduleAccessible(schedule, currentUserId);

        List<Comment> parents = commentRepository.findParentComments(scheduleId);

        List<ScheduleCommentResponse> commentResponses = createCommentTree(parents, currentUserId);

        return ScheduleCommentListResponse.from(schedule, commentResponses);
    }

    public void updateComment(Long currentUserId, Long commentId, UpdateScheduleCommentRequest request) {
        Comment comment = validateComment(commentId);

        validateCommentWriter(comment, currentUserId);

        comment.updateComment(request.content());

        publishCommentEvent(comment, CommentEventType.UPDATED, comment.getSchedule().getCommentCount());
    }

    public void deleteComment(Long currentUserId, Long commentId) {
        Comment comment = validateComment(commentId);

        validateCommentWriter(comment, currentUserId);

        comment.deleteComment();

        scheduleRepository.decreaseCommentCount(comment.getSchedule().getId());

        int commentCount = getCurrentCommentCount(comment.getSchedule().getId());

        publishCommentEvent(comment, CommentEventType.DELETED, commentCount);
    }

    private boolean isScheduleAccessible(Schedule schedule, Long currentUserId) {
        return schedule.getUser().getId().equals(currentUserId)
                || scheduleParticipantRepository.existsBySchedule_IdAndUser_Id(
                schedule.getId(), currentUserId);
    }

    private void validateScheduleAccessible(Schedule schedule, Long currentUserId) {
        if (!isScheduleAccessible(schedule, currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }
    }

    private Schedule validateSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND)
        );
    }

    private User validateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }

        return user;
    }

    private Comment validateParentComment(Long scheduleId, Long parentId) {
        Comment parent = validateComment(parentId);

        if (!parent.getSchedule().getId().equals(scheduleId)) {
            throw new BaseException(ErrorEnum.COMMENT_FORBIDDEN);
        }

        if (parent.getParent() != null) {
            throw new BaseException(ErrorEnum.INVALID_COMMENT);
        }

        return parent;
    }

    private Comment validateComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new BaseException(ErrorEnum.COMMENT_NOT_FOUND)
        );

        if (comment.isDeleted()) {
            throw new BaseException(ErrorEnum.COMMENT_ALREADY_DELETE);
        }

        return comment;
    }

    private void validateCommentWriter(Comment comment, Long currentUserId) {
        if (!comment.getWriter().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.COMMENT_FORBIDDEN);
        }
    }

    private List<ScheduleCommentResponse> createCommentTree(List<Comment> parents, Long currentUserId) {
        List<Long> parentIds = parents.stream()
                .map(Comment::getId)
                .toList();

        List<Comment> replies = commentRepository.findReplies(parentIds);

        List<ScheduleCommentResponse> replyResponses = createReplyResponses(replies, currentUserId);

        Map<Long, List<ScheduleCommentResponse>> replyMap = createReplyResponseMap(replyResponses);

        return createParentResponses(parents, currentUserId, replyMap);
    }

    private List<ScheduleCommentResponse> createReplyResponses(List<Comment> replies, Long currentUserId) {
        return replies.stream()
                .map(reply ->
                        ScheduleCommentResponse.from(
                                reply,
                                currentUserId,
                                List.of()
                        )
                )
                .toList();
    }

    private Map<Long, List<ScheduleCommentResponse>> createReplyResponseMap(List<ScheduleCommentResponse> replyResponses) {
        return replyResponses.stream()
                .collect(
                        Collectors.groupingBy(
                                ScheduleCommentResponse::parentId
                        )
                );
    }

    private List<ScheduleCommentResponse> createParentResponses(
            List<Comment> parents,
            Long currentUserId,
            Map<Long, List<ScheduleCommentResponse>> replyMap
    ) {
        return parents.stream()
                .map(parent ->
                        ScheduleCommentResponse.from(
                                parent,
                                currentUserId,
                                replyMap.getOrDefault(
                                        parent.getId(),
                                        List.of()
                                )
                        )
                )
                .toList();
    }

    private int getCurrentCommentCount(Long scheduleId) {
        return scheduleRepository.findCommentCount(scheduleId);
    }

    private void publishCommentEvent(Comment comment, CommentEventType commentEventType, int commentCount) {
        CommentEvent event = new CommentEvent(
                commentEventType,
                CommentEventResponse.from(
                        comment,
                        commentEventType,
                        commentCount
                )
        );

        commentPublisher.publish(event);
    }
}
