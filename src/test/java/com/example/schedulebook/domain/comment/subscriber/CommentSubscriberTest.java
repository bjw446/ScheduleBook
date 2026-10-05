package com.example.schedulebook.domain.comment.subscriber;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.websocket.publisher.WebSocketPublisher;
import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.enums.CommentEventType;
import com.example.schedulebook.domain.comment.event.CommentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentSubscriberTest {

    private static final Long SCHEDULE_ID = 10L;

    @Mock
    private WebSocketPublisher webSocketPublisher;

    @InjectMocks
    private CommentSubscriber commentSubscriber;

    @Test
    void onComment_댓글_이벤트를_일정_댓글_WebSocket으로_전송한다() {
        // given
        CommentEventResponse response = new CommentEventResponse(
                CommentEventType.CREATED,
                1L,
                "event-1",
                SCHEDULE_ID,
                null,
                2L,
                "작성자",
                "댓글 내용",
                false,
                false,
                LocalDateTime.now(),
                1
        );

        CommentEvent event = CommentEvent.from(response);

        String destination =
                WebSocketDestination.getScheduleCommentDestination(SCHEDULE_ID);

        // when
        commentSubscriber.onComment(event);

        // then
        verify(webSocketPublisher)
                .send(destination, event);
    }
}