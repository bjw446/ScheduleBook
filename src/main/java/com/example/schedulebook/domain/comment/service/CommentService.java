package com.example.schedulebook.domain.comment.service;

import com.example.schedulebook.domain.comment.dto.request.CreateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.request.UpdateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.dto.response.ScheduleCommentListResponse;
import com.example.schedulebook.domain.comment.dto.response.ScheduleCommentResponse;
import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.comment.enums.CommentEventType;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleValidator scheduleValidator;
    private final UserValidator userValidator;
    private final CommentValidator commentValidator;
    private final OutboxService outboxService;

    public void createComment(Long currentUserId, Long scheduleId, CreateScheduleCommentRequest request) {
        User user = userValidator.validateActiveUser(currentUserId);

        Schedule schedule = scheduleValidator.validateAccessibleSchedule(scheduleId, currentUserId);

        Comment parent = findParentComment(scheduleId, request.parentCommentId());

        Comment comment = buildComment(schedule, user, request.content(), parent);

        Comment savedComment = commentRepository.save(comment);

        scheduleRepository.increaseCommentCount(scheduleId);

        int commentCount = getCurrentCommentCount(scheduleId);

        String commentEventId = UUID.randomUUID().toString();

        CommentEventResponse commentEventResponse = CommentEventResponse.from(
                savedComment,
                commentEventId,
                CommentEventType.CREATED,
                commentCount
        );

        outboxService.save(
                commentEventId,
                OutboxAggregateType.COMMENT,
                String.valueOf(savedComment.getId()),
                OutboxEventType.COMMENT_EVENT,
                commentEventResponse
        );

        String commentCreatedEventId = UUID.randomUUID().toString();

        CommentCreatedEvent createdEvent = new CommentCreatedEvent(
                commentCreatedEventId,
                scheduleId,
                user.getId(),
                user.getNickname(),
                parent == null ? null : parent.getId()
        );

        outboxService.save(
                commentCreatedEventId,
                OutboxAggregateType.COMMENT,
                String.valueOf(savedComment.getId()),
                OutboxEventType.COMMENT_CREATED,
                createdEvent
        );
    }

    @Transactional(readOnly = true)
    public ScheduleCommentListResponse findAllComment(Long currentUserId, Long scheduleId) {
        userValidator.validateActiveUser(currentUserId);

        Schedule schedule = scheduleValidator.validateAccessibleSchedule(scheduleId, currentUserId);

        List<Comment> parents = commentRepository.findParentComments(scheduleId);

        List<ScheduleCommentResponse> commentResponses = createCommentTree(parents, currentUserId);

        return ScheduleCommentListResponse.from(schedule, commentResponses);
    }

    public void updateComment(Long currentUserId, Long commentId, UpdateScheduleCommentRequest request) {
        Comment comment = commentValidator.validateComment(commentId);

        commentValidator.validateCommentWriter(comment, currentUserId);

        comment.updateComment(request.content());

        String eventId = UUID.randomUUID().toString();

        CommentEventResponse commentEventResponse = CommentEventResponse.from(
                comment,
                eventId,
                CommentEventType.UPDATED,
                comment.getSchedule().getCommentCount()
        );

        outboxService.save(
                eventId,
                OutboxAggregateType.COMMENT,
                String.valueOf(commentId),
                OutboxEventType.COMMENT_EVENT,
                commentEventResponse
        );
    }

    public void deleteComment(Long currentUserId, Long commentId) {
        Comment comment = commentValidator.validateComment(commentId);

        commentValidator.validateCommentWriter(comment, currentUserId);

        comment.deleteComment();

        scheduleRepository.decreaseCommentCount(comment.getSchedule().getId(), 1);

        int commentCount = getCurrentCommentCount(comment.getSchedule().getId());

        String eventId = UUID.randomUUID().toString();

        CommentEventResponse commentEventResponse = CommentEventResponse.from(
                comment,
                eventId,
                CommentEventType.DELETED,
                commentCount
        );

        outboxService.save(
                eventId,
                OutboxAggregateType.COMMENT,
                String.valueOf(commentId),
                OutboxEventType.COMMENT_EVENT,
                commentEventResponse
        );
    }

    public void removeAllComments(Long userId) {
        List<Comment> comments = commentRepository.findAllByWriterId(userId);

        Map<Long, Integer> scheduleCommentCount = new HashMap<>();

        for (Comment comment : comments) {
            if (comment.isDeleted()) {
                continue;
            }

            comment.deleteComment();

            scheduleCommentCount.merge(
                    comment.getSchedule().getId(),
                    1,
                    Integer::sum
            );
        }

        scheduleCommentCount.forEach(scheduleRepository::decreaseCommentCount);
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

    private Comment findParentComment(Long scheduleId, Long parentId) {
        if (parentId == null) {
            return null;
        }

        return commentValidator.validateParentComment(scheduleId, parentId);
    }

    private Comment buildComment(Schedule schedule, User writer, String content, Comment parent) {
        if (parent == null) {
            return Comment.create(schedule, writer, content);
        }

        return Comment.reply(schedule, writer, parent, content);
    }
}
