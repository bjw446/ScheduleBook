package com.example.schedulebook.domain.scheduleparticipant.publisher;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.websocket.publisher.WebSocketPublisher;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.scheduleparticipant.service.ScheduleParticipantReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleParticipantPublisherTest {

    @Mock
    private ScheduleParticipantReader scheduleParticipantReader;

    @Mock
    private WebSocketPublisher webSocketPublisher;

    @Mock
    private ScheduleParticipantListResponse response;

    private ScheduleParticipantPublisher publisher;

    private static final Long SCHEDULE_ID = 100L;
    private static final String EXPECTED_DESTINATION = "/topic/schedule/100/participants";

    @BeforeEach
    void setUp() {
        publisher = new ScheduleParticipantPublisher(
                scheduleParticipantReader,
                webSocketPublisher
        );
    }

    @Test
    void 참여자_목록을_조회하고_일정_참여자_방에_커밋_후_발행한다() {
        // given
        when(scheduleParticipantReader.getParticipantList(SCHEDULE_ID))
                .thenReturn(response);

        try (MockedStatic<WebSocketDestination> mockedDestination =
                     org.mockito.Mockito.mockStatic(WebSocketDestination.class)) {

            mockedDestination.when(() ->
                    WebSocketDestination.getScheduleParticipantsDestination(SCHEDULE_ID)
            ).thenReturn(EXPECTED_DESTINATION);

            // when
            publisher.publishParticipantsUpdated(SCHEDULE_ID);

            // then
            verify(scheduleParticipantReader)
                    .getParticipantList(SCHEDULE_ID);

            mockedDestination.verify(() ->
                    WebSocketDestination.getScheduleParticipantsDestination(SCHEDULE_ID)
            );

            verify(webSocketPublisher).sendAfterCommit(
                    EXPECTED_DESTINATION,
                    response
            );
        }
    }
}