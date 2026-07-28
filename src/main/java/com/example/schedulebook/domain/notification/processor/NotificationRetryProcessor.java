package com.example.schedulebook.domain.notification.processor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.processor.CommentRetryProcessor;
import com.example.schedulebook.domain.friend.processor.FriendRetryProcessor;
import com.example.schedulebook.domain.notification.entity.NotificationRetry;
import com.example.schedulebook.domain.notification.service.NotificationRetryService;
import com.example.schedulebook.domain.schedule.processor.ScheduleRetryProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRetryProcessor {
    private final CommentRetryProcessor commentRetryProcessor;
    private final FriendRetryProcessor friendRetryProcessor;
    private final ScheduleRetryProcessor scheduleRetryProcessor;
    private final NotificationRetryService notificationRetryService;

    public void dispatch(Long notificationRetryId) {
        NotificationRetry notificationRetry = notificationRetryService.findById(notificationRetryId);

        switch (notificationRetry.getNotificationType()) {
            case COMMENT_REPLY, SCHEDULE_COMMENT ->
                    commentRetryProcessor.process(notificationRetry);

            case FRIEND_REQUEST, FRIEND_ACCEPTED ->
                    friendRetryProcessor.process(notificationRetry);

            case SCHEDULE_SHARED ->
                    scheduleRetryProcessor.process(notificationRetry);

            default ->
                throw new BaseException(ErrorEnum.INVALID_NOTIFICATION_TYPE);
        }
    }
}
