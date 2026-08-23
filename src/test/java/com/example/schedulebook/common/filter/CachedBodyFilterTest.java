package com.example.schedulebook.common.filter;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.security.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CachedBodyFilterTest {

    private CachedBodyFilter cachedBodyFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();

        objectMapper.registerModule(
                new JavaTimeModule()
        );

        cachedBodyFilter =
                new CachedBodyFilter(objectMapper);
    }

    @Test
    @DisplayName("로그인 요청이 아니면 원본 Request를 그대로 FilterChain에 전달한다")
    void givenNonLoginRequest_whenDoFilter_thenPassOriginalRequest()
            throws Exception {

        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setServletPath("/users");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        cachedBodyFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertSame(
                request,
                filterChain.getRequest()
        );
    }

    @Test
    @DisplayName("로그인 요청이면 CachedBodyHttpServletRequest로 감싼다")
    void givenLoginRequest_whenDoFilter_thenWrapRequest()
            throws Exception {

        // given
        String requestBody = """
                {
                    "loginId": "test123",
                    "password": "password123"
                }
                """;

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setServletPath("/auth/login");

        request.setContent(
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        cachedBodyFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertNotNull(filterChain.getRequest());

        assertInstanceOf(
                CachedBodyHttpServletRequest.class,
                filterChain.getRequest()
        );

        CachedBodyHttpServletRequest cachedRequest =
                (CachedBodyHttpServletRequest)
                        filterChain.getRequest();

        assertArrayEquals(
                requestBody.getBytes(StandardCharsets.UTF_8),
                cachedRequest.getBody()
        );
    }

    @Test
    @DisplayName("이미 CachedBodyHttpServletRequest라면 다시 wrapping하지 않는다")
    void givenAlreadyCachedRequest_whenDoFilter_thenDoNotWrapAgain()
            throws Exception {

        // given
        MockHttpServletRequest originalRequest =
                new MockHttpServletRequest();

        originalRequest.setServletPath("/auth/login");

        originalRequest.setContent(
                "test body".getBytes(StandardCharsets.UTF_8)
        );

        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(originalRequest);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        cachedBodyFilter.doFilter(
                cachedRequest,
                response,
                filterChain
        );

        // then
        assertSame(
                cachedRequest,
                filterChain.getRequest()
        );
    }

    @Test
    @DisplayName("로그인 요청의 Body를 캐싱한 Request를 FilterChain에 전달한다")
    void givenLoginRequest_whenDoFilter_thenPassCachedRequest()
            throws Exception {

        // given
        String requestBody = "schedulebook login body";

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setServletPath("/auth/login");

        request.setContent(
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        cachedBodyFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertNotNull(filterChain.getRequest());

        assertInstanceOf(
                CachedBodyHttpServletRequest.class,
                filterChain.getRequest()
        );

        CachedBodyHttpServletRequest cachedRequest =
                (CachedBodyHttpServletRequest)
                        filterChain.getRequest();

        assertEquals(
                requestBody,
                new String(
                        cachedRequest.getBody(),
                        StandardCharsets.UTF_8
                )
        );

        assertSame(
                response,
                filterChain.getResponse()
        );
    }

    @Test
    @DisplayName("Request Body가 최대 크기를 초과하면 413과 ApiResponse.fail 계약을 반환하고 FilterChain을 진행하지 않는다")
    void givenOversizedRequestBody_whenDoFilter_thenReturnErrorResponse()
            throws Exception {

        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setServletPath("/auth/login");

        request.setContent(
                new byte[RedisConst.MAX_BODY_SIZE + 1]
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        cachedBodyFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertEquals(
                ErrorEnum.REQUEST_BODY_TOO_LARGE.getStatus(),
                response.getStatus()
        );

        assertTrue(
                response.getContentType()
                        .startsWith("application/json")
        );

        assertEquals(
                "UTF-8",
                response.getCharacterEncoding()
        );

        // 응답 본문을 실제 JSON으로 파싱한다.
        String responseBody =
                response.getContentAsString();

        JsonNode json =
                objectMapper.readTree(responseBody);

        // ApiResponse.fail(ErrorEnum.REQUEST_BODY_TOO_LARGE)
        // 계약을 검증한다.
        assertFalse(
                json.get("success").asBoolean()
        );

        assertEquals(
                ErrorEnum.REQUEST_BODY_TOO_LARGE.getStatus(),
                json.get("status").asInt()
        );

        assertEquals(
                ErrorEnum.REQUEST_BODY_TOO_LARGE.getMessage(),
                json.get("message").asText()
        );

        assertNotNull(
                json.get("timestamp")
        );

        assertFalse(
                json.get("timestamp").isNull()
        );

        LocalDateTime timestamp =
                objectMapper.treeToValue(
                        json.get("timestamp"),
                        LocalDateTime.class
                );

        assertNotNull(timestamp);

        assertTrue(
                json.get("data").isNull()
        );

        // 예외가 발생했으므로 FilterChain은 진행되지 않아야 한다.
        assertNull(
                filterChain.getRequest()
        );
    }
}