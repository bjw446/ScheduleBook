package com.example.schedulebook.domain.scheduleparticipant.publisher;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.websocket.publisher.WebSocketPublisher;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleAttendanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleAttendancePublisherTest {

    @Mock
    private WebSocketPublisher webSocketPublisher;

    @Mock
    private ScheduleAttendanceResponse response;

    private ScheduleAttendancePublisher publisher;

    private static final Long SCHEDULE_ID = 1L;
    private static final String SCHEDULE_DESTINATION = "/topic/schedule/1";

    @BeforeEach
    void setUp() {
        publisher = new ScheduleAttendancePublisher(webSocketPublisher);
    }

    @Test
    void 출석_상태_변경_응답을_일정_방에_커밋_후_발행한다() {
        // given
        try (MockedStatic<WebSocketDestination> mockedDestination =
                     org.mockito.Mockito.mockStatic(WebSocketDestination.class)) {

            mockedDestination.when(() ->
                    WebSocketDestination.getScheduleDestination(SCHEDULE_ID)
            ).thenReturn(SCHEDULE_DESTINATION);

            org.mockito.Mockito.when(response.scheduleId())
                    .thenReturn(SCHEDULE_ID);

            // when
            publisher.publishAttendanceUpdated(response);

            // then
            verify(webSocketPublisher).sendAfterCommit(
                    SCHEDULE_DESTINATION,
                    response
            );

            mockedDestination.verify(() ->
                    WebSocketDestination.getScheduleDestination(SCHEDULE_ID)
            );
        }
    }
}