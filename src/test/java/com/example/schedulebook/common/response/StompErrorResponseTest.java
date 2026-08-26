package com.example.schedulebook.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StompErrorResponseTest {

    @Test
    @DisplayName("StompErrorResponse를 생성하면 code, status, message가 정상적으로 저장된다")
    void givenValues_whenCreate_thenReturnStompErrorResponse() {

        // given
        String code =
                "INVALID_INPUT";

        int status =
                400;

        String message =
                "잘못된 입력값입니다.";

        // when
        StompErrorResponse response =
                new StompErrorResponse(
                        code,
                        status,
                        message
                );

        // then
        assertNotNull(response);

        assertEquals(
                code,
                response.code()
        );

        assertEquals(
                status,
                response.status()
        );

        assertEquals(
                message,
                response.message()
        );
    }

    @Test
    @DisplayName("StompErrorResponse의 JSON 필드명이 code, status, message로 직렬화된다")
    void givenStompErrorResponse_whenSerialize_thenReturnExpectedJson()
            throws Exception {

        // given
        StompErrorResponse response =
                new StompErrorResponse(
                        "INVALID_INPUT",
                        400,
                        "잘못된 입력값입니다."
                );

        com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        // when
        String json =
                objectMapper.writeValueAsString(response);

        // then
        assertTrue(
                json.contains("\"code\":\"INVALID_INPUT\"")
        );

        assertTrue(
                json.contains("\"status\":400")
        );

        assertTrue(
                json.contains("\"message\":\"잘못된 입력값입니다.\"")
        );
    }
}