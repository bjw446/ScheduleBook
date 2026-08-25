package com.example.schedulebook.common.config;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.exception.SessionLimitException;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.dto.response.SessionLimitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler =
                new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("BaseException 발생 시 ErrorEnum의 status와 message를 응답한다")
    void givenBaseException_whenHandle_thenReturnErrorResponse() {

        // given
        BaseException exception =
                new BaseException(
                        ErrorEnum.INVALID_INPUT
                );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleBaseException(
                        exception
                );

        // then
        assertEquals(
                ErrorEnum.INVALID_INPUT.getStatus(),
                response.getStatusCode().value()
        );

        assertNotNull(
                response.getBody()
        );

        ApiResponse<Void> body =
                response.getBody();

        assertFalse(
                body.success()
        );

        assertEquals(
                ErrorEnum.INVALID_INPUT.getStatus(),
                body.status()
        );

        assertEquals(
                ErrorEnum.INVALID_INPUT.getMessage(),
                body.message()
        );

        assertNotNull(
                body.timestamp()
        );

        assertNull(
                body.data()
        );
    }

    @Test
    @DisplayName("Validation 오류 발생 시 INVALID_ARGUMENT와 FieldError 메시지를 응답한다")
    void givenValidationException_whenHandle_thenReturnInvalidArgument() {

        // given
        String validationMessage =
                "로그인 ID는 필수입니다.";

        FieldError fieldError =
                new FieldError(
                        "loginRequest",
                        "loginId",
                        validationMessage
                );

        BindingResult bindingResult =
                mock(BindingResult.class);

        when(
                bindingResult.getFieldErrors()
        ).thenReturn(
                List.of(fieldError)
        );

        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        when(
                exception.getBindingResult()
        ).thenReturn(
                bindingResult
        );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleValidationException(
                        exception
                );

        // then
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                response.getStatusCode().value()
        );

        assertNotNull(
                response.getBody()
        );

        ApiResponse<Void> body =
                response.getBody();

        assertFalse(
                body.success()
        );

        assertEquals(
                ErrorEnum.INVALID_ARGUMENT.getStatus(),
                body.status()
        );

        assertEquals(
                validationMessage,
                body.message()
        );

        assertNotNull(
                body.timestamp()
        );

        assertNull(
                body.data()
        );
    }

    @Test
    @DisplayName("Validation 메시지가 없으면 INVALID_INPUT 메시지를 기본값으로 사용한다")
    void givenValidationExceptionWithoutMessage_whenHandle_thenUseDefaultMessage() {

        // given
        BindingResult bindingResult =
                mock(BindingResult.class);

        when(
                bindingResult.getFieldErrors()
        ).thenReturn(
                List.of()
        );

        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        when(
                exception.getBindingResult()
        ).thenReturn(
                bindingResult
        );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleValidationException(
                        exception
                );

        // then
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                response.getStatusCode().value()
        );

        assertNotNull(
                response.getBody()
        );

        ApiResponse<Void> body =
                response.getBody();

        assertFalse(
                body.success()
        );

        assertEquals(
                ErrorEnum.INVALID_ARGUMENT.getStatus(),
                body.status()
        );

        assertEquals(
                ErrorEnum.INVALID_INPUT.getMessage(),
                body.message()
        );

        assertNotNull(
                body.timestamp()
        );

        assertNull(
                body.data()
        );
    }

    @Test
    @DisplayName("잘못된 JSON 요청이면 INVALID_INPUT을 응답한다")
    void givenHttpMessageNotReadableException_whenHandle_thenReturnInvalidInput() {

        // given
        HttpMessageNotReadableException exception =
                mock(
                        HttpMessageNotReadableException.class
                );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleHttpMessageNotReadable(
                        exception
                );

        // then
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                response.getStatusCode().value()
        );

        assertErrorResponse(
                response,
                ErrorEnum.INVALID_INPUT
        );
    }

    @Test
    @DisplayName("ConstraintViolationException 발생 시 INVALID_INPUT을 응답한다")
    void givenConstraintViolationException_whenHandle_thenReturnInvalidInput() {

        // given
        ConstraintViolationException exception =
                mock(
                        ConstraintViolationException.class
                );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleConstraintViolation(
                        exception
                );

        // then
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                response.getStatusCode().value()
        );

        assertErrorResponse(
                response,
                ErrorEnum.INVALID_INPUT
        );
    }

    @Test
    @DisplayName("요청 파라미터 타입이 잘못되면 INVALID_INPUT을 응답한다")
    void givenTypeMismatchException_whenHandle_thenReturnInvalidInput() {

        // given
        MethodArgumentTypeMismatchException exception =
                mock(
                        MethodArgumentTypeMismatchException.class
                );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleTypeMismatch(
                        exception
                );

        // then
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                response.getStatusCode().value()
        );

        assertErrorResponse(
                response,
                ErrorEnum.INVALID_INPUT
        );
    }

    @Test
    @DisplayName("DB Unique 제약조건 위반 시 DATA_CONFLICT와 409를 응답한다")
    void givenUniqueConstraintViolation_whenHandle_thenReturnDataConflict() {

        // given
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "could not execute statement",
                        new RuntimeException(
                                "duplicate key value violates unique constraint"
                        )
                );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleDataIntegrity(
                        exception
                );

        // then
        assertEquals(
                HttpStatus.CONFLICT.value(),
                response.getStatusCode().value()
        );

        assertErrorResponse(
                response,
                ErrorEnum.DATA_CONFLICT
        );
    }

    @Test
    @DisplayName("DB Unique 제약조건이 아닌 오류는 INVALID_INPUT과 400을 응답한다")
    void givenOtherDataIntegrityViolation_whenHandle_thenReturnInvalidInput() {

        // given
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "could not execute statement",
                        new RuntimeException(
                                "foreign key constraint violation"
                        )
                );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleDataIntegrity(
                        exception
                );

        // then
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                response.getStatusCode().value()
        );

        assertErrorResponse(
                response,
                ErrorEnum.INVALID_INPUT
        );
    }

    @Test
    @DisplayName("예상하지 못한 Exception 발생 시 500 INTERNAL_SERVER_ERROR를 응답한다")
    void givenUnexpectedException_whenHandle_thenReturnInternalServerError() {

        // given
        Exception exception =
                new RuntimeException(
                        "unexpected error"
                );

        // when
        ResponseEntity<ApiResponse<Void>> response =
                globalExceptionHandler.handleException(
                        exception
                );

        // then
        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                response.getStatusCode().value()
        );

        assertErrorResponse(
                response,
                ErrorEnum.INTERNAL_SERVER_ERROR
        );
    }

    @Test
    @DisplayName("SessionLimitException 발생 시 세션 정보와 함께 응답한다")
    void givenSessionLimitException_whenHandle_thenReturnSessionLimitResponse()
            throws Exception {

        // given
        LocalDateTime loginAt =
                LocalDateTime.of(
                        2026,
                        8,
                        25,
                        9,
                        0
                );

        LocalDateTime lastAccessAt =
                LocalDateTime.of(
                        2026,
                        8,
                        25,
                        9,
                        30
                );

        SessionInfoResponse sessionInfo =
                new SessionInfoResponse(
                        "session-123",
                        "127.0.0.1",
                        "Mozilla/5.0",
                        loginAt,
                        lastAccessAt
                );

        List<SessionInfoResponse> sessions =
                List.of(sessionInfo);

        SessionLimitException exception =
                new SessionLimitException(
                        ErrorEnum.SESSION_LIMIT_EXCEEDED,
                        sessions
                );

        // when
        ResponseEntity<ApiResponse<SessionLimitResponse>> response =
                globalExceptionHandler.handleSessionLimitException(
                        exception
                );

        // then

        // HTTP Status
        assertEquals(
                ErrorEnum.SESSION_LIMIT_EXCEEDED.getStatus(),
                response.getStatusCode().value()
        );

        assertNotNull(
                response.getBody()
        );

        ApiResponse<SessionLimitResponse> body =
                response.getBody();

        // ApiResponse 기본 계약
        assertFalse(
                body.success()
        );

        assertEquals(
                ErrorEnum.SESSION_LIMIT_EXCEEDED.getStatus(),
                body.status()
        );

        assertEquals(
                ErrorEnum.SESSION_LIMIT_EXCEEDED.getMessage(),
                body.message()
        );

        assertNotNull(
                body.timestamp()
        );

        // SessionLimitResponse 존재 확인
        assertNotNull(
                body.data()
        );

        // 세션 정보 존재 확인
        assertNotNull(
                body.data().sessionInfoResponses()
        );

        assertEquals(
                1,
                body.data().sessionInfoResponses().size()
        );

        // 실제 세션 정보 검증
        SessionInfoResponse actual =
                body.data()
                        .sessionInfoResponses()
                        .get(0);

        assertEquals(
                "session-123",
                actual.sessionId()
        );

        assertEquals(
                "127.0.0.1",
                actual.ip()
        );

        assertEquals(
                "Mozilla/5.0",
                actual.userAgent()
        );

        assertEquals(
                loginAt,
                actual.loginAt()
        );

        assertEquals(
                lastAccessAt,
                actual.lastAccessAt()
        );
    }

    /**
     * 일반적인 ErrorEnum 기반 ApiResponse 계약 검증
     */
    private void assertErrorResponse(
            ResponseEntity<ApiResponse<Void>> response,
            ErrorEnum errorEnum
    ) {

        assertNotNull(
                response.getBody()
        );

        ApiResponse<Void> body =
                response.getBody();

        assertFalse(
                body.success()
        );

        assertEquals(
                errorEnum.getStatus(),
                body.status()
        );

        assertEquals(
                errorEnum.getMessage(),
                body.message()
        );

        assertNotNull(
                body.timestamp()
        );

        assertNull(
                body.data()
        );
    }
}
