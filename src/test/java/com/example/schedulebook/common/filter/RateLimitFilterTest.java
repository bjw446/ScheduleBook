package com.example.schedulebook.common.filter;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.service.RedisRateLimitService;
import com.example.schedulebook.common.security.CachedBodyHttpServletRequest;
import com.example.schedulebook.domain.auth.dto.request.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private RedisRateLimitService redisRateLimitService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        redisRateLimitService =
                mock(RedisRateLimitService.class);

        objectMapper =
                new ObjectMapper();

        objectMapper.registerModule(
                new JavaTimeModule()
        );

        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        rateLimitFilter =
                new RateLimitFilter(
                        redisRateLimitService,
                        objectMapper
                );
    }

    @Test
    @DisplayName("로그인 요청이 아니면 RateLimitFilter를 적용하지 않는다")
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
        rateLimitFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertSame(
                request,
                filterChain.getRequest()
        );

        verifyNoInteractions(
                redisRateLimitService
        );
    }

    @Test
    @DisplayName("로그인 요청이지만 CachedBodyHttpServletRequest가 아니면 원본 Request를 그대로 전달한다")
    void givenLoginRequestWithoutCachedBody_whenDoFilter_thenPassOriginalRequest()
            throws Exception {

        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setServletPath("/auth/login");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        rateLimitFilter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertSame(
                request,
                filterChain.getRequest()
        );

        verifyNoInteractions(
                redisRateLimitService
        );
    }

    @Test
    @DisplayName("정상적인 로그인 요청이면 IP와 Login ID 모두 Rate Limit을 검사하고 FilterChain을 진행한다")
    void givenValidLoginRequest_whenDoFilter_thenValidateIpAndLoginId()
            throws Exception {

        // given
        String loginId = "test123";
        String password = "Password123!";

        CachedBodyHttpServletRequest cachedRequest =
                createCachedLoginRequest(
                        loginId,
                        password,
                        "127.0.0.1"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        when(
                redisRateLimitService.allowRequest(
                        anyString(),
                        anyLong(),
                        anyLong(),
                        anyString()
                )
        ).thenReturn(true);

        // when
        rateLimitFilter.doFilter(
                cachedRequest,
                response,
                filterChain
        );

        // then
        assertSame(
                cachedRequest,
                filterChain.getRequest()
        );

        assertSame(
                response,
                filterChain.getResponse()
        );

        verify(
                redisRateLimitService
        ).allowRequest(
                eq(RedisConst.LOGIN_IP_PREFIX + "127.0.0.1"),
                eq(60_000L),
                eq(5L),
                anyString()
        );

        verify(
                redisRateLimitService
        ).allowRequest(
                eq(RedisConst.LOGIN_ID_PREFIX + loginId),
                eq(60_000L),
                eq(5L),
                anyString()
        );
    }

    @Test
    @DisplayName("IP Rate Limit을 초과하면 429 응답을 반환하고 FilterChain을 진행하지 않는다")
    void givenIpRateLimitExceeded_whenDoFilter_thenReturn429()
            throws Exception {

        // given
        String loginId = "test123";

        CachedBodyHttpServletRequest cachedRequest =
                createCachedLoginRequest(
                        loginId,
                        "Password123!",
                        "127.0.0.1"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        when(
                redisRateLimitService.allowRequest(
                        eq(RedisConst.LOGIN_IP_PREFIX + "127.0.0.1"),
                        eq(60_000L),
                        eq(5L),
                        anyString()
                )
        ).thenReturn(false);

        // when
        rateLimitFilter.doFilter(
                cachedRequest,
                response,
                filterChain
        );

        // then
        assertRateLimitResponse(
                response,
                ErrorEnum.LOGIN_IP_RATE_LIMITED
        );

        // IP까지만 검사
        verify(
                redisRateLimitService
        ).allowRequest(
                eq(RedisConst.LOGIN_IP_PREFIX + "127.0.0.1"),
                eq(60_000L),
                eq(5L),
                anyString()
        );

        verify(
                redisRateLimitService,
                never()
        ).allowRequest(
                eq(RedisConst.LOGIN_ID_PREFIX + loginId),
                anyLong(),
                anyLong(),
                anyString()
        );

        // FilterChain 진행 금지
        assertNull(
                filterChain.getRequest()
        );
    }

    @Test
    @DisplayName("Login ID Rate Limit을 초과하면 429 응답을 반환하고 FilterChain을 진행하지 않는다")
    void givenLoginIdRateLimitExceeded_whenDoFilter_thenReturn429()
            throws Exception {

        // given
        String loginId = "test123";

        CachedBodyHttpServletRequest cachedRequest =
                createCachedLoginRequest(
                        loginId,
                        "Password123!",
                        "127.0.0.1"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // IP 통과
        when(
                redisRateLimitService.allowRequest(
                        eq(RedisConst.LOGIN_IP_PREFIX + "127.0.0.1"),
                        eq(60_000L),
                        eq(5L),
                        anyString()
                )
        ).thenReturn(true);

        // Login ID 차단
        when(
                redisRateLimitService.allowRequest(
                        eq(RedisConst.LOGIN_ID_PREFIX + loginId),
                        eq(60_000L),
                        eq(5L),
                        anyString()
                )
        ).thenReturn(false);

        // when
        rateLimitFilter.doFilter(
                cachedRequest,
                response,
                filterChain
        );

        // then
        assertRateLimitResponse(
                response,
                ErrorEnum.LOGIN_ID_RATE_LIMITED
        );

        // IP 검사
        verify(
                redisRateLimitService
        ).allowRequest(
                eq(RedisConst.LOGIN_IP_PREFIX + "127.0.0.1"),
                eq(60_000L),
                eq(5L),
                anyString()
        );

        // Login ID 검사
        verify(
                redisRateLimitService
        ).allowRequest(
                eq(RedisConst.LOGIN_ID_PREFIX + loginId),
                eq(60_000L),
                eq(5L),
                anyString()
        );

        // FilterChain 진행 금지
        assertNull(
                filterChain.getRequest()
        );
    }

    @Test
    @DisplayName("Redis 오류가 발생하면 REDIS_UNAVAILABLE 예외가 발생한다")
    void givenRedisUnavailable_whenDoFilter_thenThrowRedisUnavailable()
            throws Exception {

        // given
        CachedBodyHttpServletRequest cachedRequest =
                createCachedLoginRequest(
                        "test123",
                        "Password123!",
                        "127.0.0.1"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        BaseException redisUnavailable =
                new BaseException(
                        ErrorEnum.REDIS_UNAVAILABLE
                );

        when(
                redisRateLimitService.allowRequest(
                        anyString(),
                        anyLong(),
                        anyLong(),
                        anyString()
                )
        ).thenThrow(redisUnavailable);

        // when
        BaseException exception =
                assertThrows(
                        BaseException.class,
                        () -> rateLimitFilter.doFilter(
                                cachedRequest,
                                response,
                                filterChain
                        )
                );

        // then
        assertEquals(
                ErrorEnum.REDIS_UNAVAILABLE,
                exception.getErrorEnum()
        );

        // Redis 오류가 발생했으므로 FilterChain 진행 금지
        assertNull(
                filterChain.getRequest()
        );
    }

    @Test
    @DisplayName("잘못된 JSON Body이면 INVALID_INPUT 예외가 발생하고 FilterChain을 진행하지 않는다")
    void givenInvalidJsonBody_whenDoFilter_thenThrowInvalidInput()
            throws Exception {

        // given
        String invalidJson =
                """
                {
                    "loginId": "test123",
                    "password":
                }
                """;

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setServletPath("/auth/login");
        request.setRemoteAddr("127.0.0.1");

        request.setContent(
                invalidJson.getBytes(StandardCharsets.UTF_8)
        );

        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        BaseException exception =
                assertThrows(
                        BaseException.class,
                        () -> rateLimitFilter.doFilter(
                                cachedRequest,
                                response,
                                filterChain
                        )
                );

        // then

        // INVALID_INPUT으로 변환되었는지 검증
        assertEquals(
                ErrorEnum.INVALID_INPUT,
                exception.getErrorEnum()
        );

        // JSON 파싱 단계에서 실패했으므로 Redis Rate Limit 검사는 실행되면 안 된다.
        verifyNoInteractions(
                redisRateLimitService
        );

        // 예외가 발생했으므로 FilterChain도 진행되면 안 된다.
        assertNull(
                filterChain.getRequest()
        );
    }

    /**
     * 로그인 요청을 CachedBodyHttpServletRequest로 생성한다.
     */
    private CachedBodyHttpServletRequest createCachedLoginRequest(
            String loginId,
            String password,
            String ip
    ) throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        new LoginRequest(
                                loginId,
                                password,
                                null
                        )
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setServletPath("/auth/login");

        request.setRemoteAddr(ip);

        request.setContent(
                requestBody.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        return new CachedBodyHttpServletRequest(
                request
        );
    }

    /**
     * Rate Limit 차단 응답의 공통 API 계약을 검증한다.
     */
    private void assertRateLimitResponse(
            MockHttpServletResponse response,
            ErrorEnum errorEnum
    ) throws Exception {

        assertEquals(
                errorEnum.getStatus(),
                response.getStatus()
        );

        assertNotNull(
                response.getContentType()
        );

        assertTrue(
                response.getContentType()
                        .startsWith("application/json")
        );

        assertEquals(
                "UTF-8",
                response.getCharacterEncoding()
        );

        String responseBody =
                response.getContentAsString();

        assertFalse(
                responseBody.isBlank()
        );

        JsonNode json =
                objectMapper.readTree(responseBody);

        assertFalse(
                json.get("success").asBoolean()
        );

        assertEquals(
                errorEnum.getStatus(),
                json.get("status").asInt()
        );

        assertEquals(
                errorEnum.getMessage(),
                json.get("message").asText()
        );

        assertTrue(
                json.get("timestamp").isTextual()
        );

        assertFalse(
                json.get("timestamp").asText().isBlank()
        );

        assertTrue(
                json.get("data").isNull()
        );
    }
}