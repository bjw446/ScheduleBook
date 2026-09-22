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

    private static final Long RESPONSE_SCHEDULE_ID = 100L;
    private static final String EXPECTED_SCHEDULE_DESTINATION = "/topic/schedule/100";

    @BeforeEach
    void setUp() {
        publisher = new ScheduleAttendancePublisher(webSocketPublisher);
    }

    @Test
    void 출석_상태_변경_응답을_응답의_일정_ID를_기반으로_커밋_후_발행한다() {
        // given
        try (MockedStatic<WebSocketDestination> mockedDestination =
                     org.mockito.Mockito.mockStatic(WebSocketDestination.class)) {

            org.mockito.Mockito.when(response.scheduleId())
                    .thenReturn(RESPONSE_SCHEDULE_ID);

            mockedDestination.when(() ->
                    WebSocketDestination.getScheduleDestination(RESPONSE_SCHEDULE_ID)
            ).thenReturn(EXPECTED_SCHEDULE_DESTINATION);

            // when
            publisher.publishAttendanceUpdated(response);

            // then
            verify(response).scheduleId();

            mockedDestination.verify(() ->
                    WebSocketDestination.getScheduleDestination(RESPONSE_SCHEDULE_ID)
            );

            verify(webSocketPublisher).sendAfterCommit(
                    EXPECTED_SCHEDULE_DESTINATION,
                    response
            );
        }
    }
}