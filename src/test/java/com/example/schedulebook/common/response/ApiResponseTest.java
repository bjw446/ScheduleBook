package com.example.schedulebook.common.response;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.domain.auth.dto.response.SessionLimitResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        objectMapper =
                new ObjectMapper();

        objectMapper.registerModule(
                new JavaTimeModule()
        );

        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );
    }

    @Test
    @DisplayName("성공 응답을 생성하면 SuccessEnum의 status와 message, data가 정상적으로 설정된다")
    void givenSuccessEnumAndData_whenSuccess_thenReturnSuccessResponse()
            throws Exception {

        // given
        SuccessEnum successEnum =
                SuccessEnum.CREATE_SUCCESS;

        String data =
                "test-data";

        // when
        ApiResponse<String> response =
                ApiResponse.success(
                        successEnum,
                        data
                );

        // then
        assertTrue(
                response.success()
        );

        assertEquals(
                successEnum.getStatus(),
                response.status()
        );

        assertEquals(
                successEnum.getMessage(),
                response.message()
        );

        assertEquals(
                data,
                response.data()
        );

        assertNotNull(
                response.timestamp()
        );
    }

    @Test
    @DisplayName("실패 응답을 생성하면 ErrorEnum의 status와 message가 정상적으로 설정된다")
    void givenErrorEnum_whenFail_thenReturnFailureResponse() {

        // given
        ErrorEnum errorEnum =
                ErrorEnum.INVALID_INPUT;

        // when
        ApiResponse<Void> response =
                ApiResponse.fail(
                        errorEnum
                );

        // then
        assertFalse(
                response.success()
        );

        assertEquals(
                errorEnum.getStatus(),
                response.status()
        );

        assertEquals(
                errorEnum.getMessage(),
                response.message()
        );

        assertNull(
                response.data()
        );

        assertNotNull(
                response.timestamp()
        );
    }

    @Test
    @DisplayName("커스텀 message를 전달한 실패 응답은 ErrorEnum의 status와 커스텀 message를 사용한다")
    void givenErrorEnumAndCustomMessage_whenFail_thenUseCustomMessage() {

        // given
        ErrorEnum errorEnum =
                ErrorEnum.INVALID_ARGUMENT;

        String customMessage =
                "로그인 ID는 필수입니다.";

        // when
        ApiResponse<Void> response =
                ApiResponse.fail(
                        errorEnum,
                        customMessage
                );

        // then
        assertFalse(
                response.success()
        );

        assertEquals(
                errorEnum.getStatus(),
                response.status()
        );

        assertEquals(
                customMessage,
                response.message()
        );

        assertNull(
                response.data()
        );

        assertNotNull(
                response.timestamp()
        );
    }

    @Test
    @DisplayName("실패 응답에 객체 data를 전달하면 ErrorEnum의 message와 data를 함께 반환한다")
    void givenErrorEnumAndData_whenFail_thenReturnData() {

        // given
        ErrorEnum errorEnum =
                ErrorEnum.INVALID_INPUT;

        SessionLimitResponse data =
                new SessionLimitResponse(
                        List.of()
                );

        // when
        ApiResponse<SessionLimitResponse> response =
                ApiResponse.fail(
                        errorEnum,
                        data
                );

        // then
        assertFalse(
                response.success()
        );

        assertEquals(
                errorEnum.getStatus(),
                response.status()
        );

        assertEquals(
                errorEnum.getMessage(),
                response.message()
        );

        assertSame(
                data,
                response.data()
        );

        assertNotNull(
                response.timestamp()
        );
    }

    @Test
    @DisplayName("성공 응답은 지정된 JSON 필드명과 순서로 직렬화된다")
    void givenSuccessResponse_whenSerialize_thenReturnExpectedJsonContract()
            throws Exception {

        // given
        ApiResponse<String> response =
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        "test-data"
                );

        // when
        String json =
                objectMapper.writeValueAsString(
                        response
                );

        JsonNode root =
                objectMapper.readTree(
                        json
                );

        // then
        assertTrue(
                root.get("success").asBoolean()
        );

        assertEquals(
                SuccessEnum.CREATE_SUCCESS.getStatus(),
                root.get("status").asInt()
        );

        assertEquals(
                SuccessEnum.CREATE_SUCCESS.getMessage(),
                root.get("message").asText()
        );

        assertEquals(
                "test-data",
                root.get("data").asText()
        );

        assertTrue(
                root.get("timestamp").isTextual()
        );

        assertFalse(
                root.get("timestamp").asText().isBlank()
        );
    }

    @Test
    @DisplayName("실패 응답 JSON의 data는 null로 직렬화된다")
    void givenFailureResponse_whenSerialize_thenDataIsNull()
            throws Exception {

        // given
        ApiResponse<Void> response =
                ApiResponse.fail(
                        ErrorEnum.INVALID_INPUT
                );

        // when
        String json =
                objectMapper.writeValueAsString(
                        response
                );

        JsonNode root =
                objectMapper.readTree(
                        json
                );

        // then
        assertFalse(
                root.get("success").asBoolean()
        );

        assertEquals(
                ErrorEnum.INVALID_INPUT.getStatus(),
                root.get("status").asInt()
        );

        assertEquals(
                ErrorEnum.INVALID_INPUT.getMessage(),
                root.get("message").asText()
        );

        assertTrue(
                root.get("data").isNull()
        );

        assertTrue(
                root.get("timestamp").isTextual()
        );

        assertFalse(
                root.get("timestamp").asText().isBlank()
        );
    }
}
