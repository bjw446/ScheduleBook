package com.example.schedulebook.common.config;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.StompErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketExceptionHandlerTest {

    private WebSocketExceptionHandler webSocketExceptionHandler;

    @BeforeEach
    void setUp() {
        webSocketExceptionHandler =
                new WebSocketExceptionHandler();
    }

    @Test
    @DisplayName("WebSocket에서 BaseException 발생 시 StompErrorResponse로 변환한다")
    void givenBaseException_whenHandle_thenReturnStompErrorResponse() {

        // given
        BaseException exception =
                new BaseException(
                        ErrorEnum.INVALID_INPUT
                );

        // when
        StompErrorResponse response =
                webSocketExceptionHandler.handleBaseException(
                        exception
                );

        // then
        assertNotNull(response);

        assertEquals(
                ErrorEnum.INVALID_INPUT.name(),
                response.code()
        );

        assertEquals(
                ErrorEnum.INVALID_INPUT.getStatus(),
                response.status()
        );

        assertEquals(
                ErrorEnum.INVALID_INPUT.getMessage(),
                response.message()
        );
    }

    @Test
    @DisplayName("WebSocket에서 다른 BaseException도 ErrorEnum의 code/status/message를 정확하게 반환한다")
    void givenAnotherBaseException_whenHandle_thenReturnErrorEnumValues() {

        // given
        BaseException exception =
                new BaseException(
                        ErrorEnum.REDIS_UNAVAILABLE
                );

        // when
        StompErrorResponse response =
                webSocketExceptionHandler.handleBaseException(
                        exception
                );

        // then
        assertNotNull(response);

        assertEquals(
                ErrorEnum.REDIS_UNAVAILABLE.name(),
                response.code()
        );

        assertEquals(
                ErrorEnum.REDIS_UNAVAILABLE.getStatus(),
                response.status()
        );

        assertEquals(
                ErrorEnum.REDIS_UNAVAILABLE.getMessage(),
                response.message()
        );
    }
}
