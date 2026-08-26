package com.example.schedulebook.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonMessageUtilsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper =
                new ObjectMapper();
    }

    @Test
    @DisplayName("정상적인 JSON에 eventId가 있으면 eventId를 반환한다")
    void givenValidJsonWithEventId_whenExtract_thenReturnEventId() {

        // given
        String message =
                """
                {
                    "eventId": "event-123",
                    "type": "ORDER_CREATED"
                }
                """;

        // when
        String eventId =
                JsonMessageUtils.extractEventId(
                        objectMapper,
                        message
                );

        // then
        assertEquals(
                "event-123",
                eventId
        );
    }

    @Test
    @DisplayName("JSON에 eventId가 없으면 null을 반환한다")
    void givenJsonWithoutEventId_whenExtract_thenReturnNull() {

        // given
        String message =
                """
                {
                    "type": "ORDER_CREATED"
                }
                """;

        // when
        String eventId =
                JsonMessageUtils.extractEventId(
                        objectMapper,
                        message
                );

        // then
        assertNull(eventId);
    }

    @Test
    @DisplayName("eventId가 null이면 null을 반환한다")
    void givenJsonWithNullEventId_whenExtract_thenReturnNull() {

        // given
        String message =
                """
                {
                    "eventId": null
                }
                """;

        // when
        String eventId =
                JsonMessageUtils.extractEventId(
                        objectMapper,
                        message
                );

        // then
        assertNull(eventId);
    }

    @Test
    @DisplayName("잘못된 JSON이면 예외를 발생시키지 않고 null을 반환한다")
    void givenInvalidJson_whenExtract_thenReturnNull() {

        // given
        String message =
                """
                {
                    "eventId": "event-123",
                """;

        // when
        String eventId =
                assertDoesNotThrow(
                        () -> JsonMessageUtils.extractEventId(
                                objectMapper,
                                message
                        )
                );

        // then
        assertNull(eventId);
    }

    @Test
    @DisplayName("eventId가 숫자 타입이어도 문자열로 변환해서 반환한다")
    void givenNumericEventId_whenExtract_thenReturnString() {

        // given
        String message =
                """
                {
                    "eventId": 12345
                }
                """;

        // when
        String eventId =
                JsonMessageUtils.extractEventId(
                        objectMapper,
                        message
                );

        // then
        assertEquals(
                "12345",
                eventId
        );
    }
}