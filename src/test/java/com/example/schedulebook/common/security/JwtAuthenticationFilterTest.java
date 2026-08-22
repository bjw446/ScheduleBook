package com.example.schedulebook.common.security;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.service.RedisBlacklistService;
import com.example.schedulebook.common.redis.service.RedisSessionService;
import com.example.schedulebook.domain.auth.service.SessionBlockStore;
import com.example.schedulebook.domain.user.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "session-123";
    private static final UserRole USER_ROLE = UserRole.USER;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedisBlacklistService redisBlacklistService;

    @Mock
    private RedisSessionService redisSessionService;

    @Mock
    private SessionBlockStore sessionBlockStore;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter responseWriter;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() throws IOException {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtProvider,
                objectMapper,
                redisBlacklistService,
                redisSessionService,
                sessionBlockStore
        );

        lenient().when(response.getWriter()).thenReturn(responseWriter);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정상 Bearer Token 인증 성공")
    void givenValidBearerToken_whenFilter_thenAuthenticateUser()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + TOKEN);

        when(redisBlacklistService.isBlacklisted(TOKEN))
                .thenReturn(false);

        when(jwtProvider.extractUserId(TOKEN))
                .thenReturn(USER_ID);

        when(jwtProvider.extractSessionId(TOKEN))
                .thenReturn(SESSION_ID);

        when(jwtProvider.extractUserRole(TOKEN))
                .thenReturn(USER_ROLE);

        when(sessionBlockStore.isBlocked(SESSION_ID))
                .thenReturn(false);

        when(redisSessionService.existsSession(SESSION_ID))
                .thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);

        assertTrue(
                authentication.getPrincipal() instanceof UserPrincipal
        );

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        assertEquals(USER_ID, principal.userId());
        assertEquals(USER_ROLE, principal.userRole());

        assertEquals(
                1,
                authentication.getAuthorities().size()
        );

        assertEquals(
                new SimpleGrantedAuthority("ROLE_USER"),
                authentication.getAuthorities().iterator().next()
        );

        verify(redisBlacklistService)
                .isBlacklisted(TOKEN);

        verify(jwtProvider)
                .validateToken(TOKEN);

        verify(jwtProvider)
                .extractUserId(TOKEN);

        verify(jwtProvider)
                .extractSessionId(TOKEN);

        verify(jwtProvider)
                .extractUserRole(TOKEN);

        verify(sessionBlockStore)
                .isBlocked(SESSION_ID);

        verify(redisSessionService)
                .existsSession(SESSION_ID);

        verify(redisSessionService)
                .updateLastAccess(SESSION_ID);

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization Header가 없으면 인증하지 않음")
    void givenNoAuthorizationHeader_whenFilter_thenDoNotAuthenticate()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);

        verifyNoInteractions(jwtProvider);
        verifyNoInteractions(redisBlacklistService);
        verifyNoInteractions(redisSessionService);
        verifyNoInteractions(sessionBlockStore);
    }

    @Test
    @DisplayName("Bearer Token 형식이 아니면 인증하지 않음")
    void givenNonBearerAuthorizationHeader_whenFilter_thenDoNotAuthenticate()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Basic abcdef");

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);

        verifyNoInteractions(jwtProvider);
        verifyNoInteractions(redisBlacklistService);
        verifyNoInteractions(redisSessionService);
        verifyNoInteractions(sessionBlockStore);
    }

    @Test
    @DisplayName("Blacklist Token이면 인증 거부")
    void givenBlacklistedToken_whenFilter_thenRejectAuthentication()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + TOKEN);

        when(redisBlacklistService.isBlacklisted(TOKEN))
                .thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(redisBlacklistService)
                .isBlacklisted(TOKEN);

        verify(response)
                .setStatus(ErrorEnum.LOGOUT_TOKEN.getStatus());

        verify(jwtProvider, never())
                .validateToken(anyString());

        verify(redisSessionService, never())
                .existsSession(anyString());

        verify(filterChain, never())
                .doFilter(any(), any());
    }

    @Test
    @DisplayName("차단된 Session이면 FORCE_LOGOUT으로 인증 거부")
    void givenBlockedSession_whenFilter_thenRejectAuthentication()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + TOKEN);

        when(redisBlacklistService.isBlacklisted(TOKEN))
                .thenReturn(false);

        when(jwtProvider.extractUserId(TOKEN))
                .thenReturn(USER_ID);

        when(jwtProvider.extractSessionId(TOKEN))
                .thenReturn(SESSION_ID);

        when(sessionBlockStore.isBlocked(SESSION_ID))
                .thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(jwtProvider)
                .validateToken(TOKEN);

        verify(jwtProvider)
                .extractUserId(TOKEN);

        verify(jwtProvider)
                .extractSessionId(TOKEN);

        verify(sessionBlockStore)
                .isBlocked(SESSION_ID);

        verify(response)
                .setStatus(ErrorEnum.FORCE_LOGOUT.getStatus());

        verify(jwtProvider, never())
                .extractUserRole(TOKEN);

        verify(redisSessionService, never())
                .existsSession(anyString());

        verify(redisSessionService, never())
                .updateLastAccess(anyString());

        verify(filterChain, never())
                .doFilter(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 Session이면 SESSION_NOT_FOUND로 인증 거부")
    void givenNonExistentSession_whenFilter_thenRejectAuthentication()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + TOKEN);

        when(redisBlacklistService.isBlacklisted(TOKEN))
                .thenReturn(false);

        when(jwtProvider.extractUserId(TOKEN))
                .thenReturn(USER_ID);

        when(jwtProvider.extractSessionId(TOKEN))
                .thenReturn(SESSION_ID);

        when(sessionBlockStore.isBlocked(SESSION_ID))
                .thenReturn(false);

        when(jwtProvider.extractUserRole(TOKEN))
                .thenReturn(USER_ROLE);

        when(redisSessionService.existsSession(SESSION_ID))
                .thenReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(redisSessionService)
                .existsSession(SESSION_ID);

        verify(response)
                .setStatus(ErrorEnum.SESSION_NOT_FOUND.getStatus());

        verify(redisSessionService, never())
                .updateLastAccess(anyString());

        verify(filterChain, never())
                .doFilter(any(), any());
    }

    @Test
    @DisplayName("Session 마지막 접근시간 갱신에 실패해도 인증 성공")
    void givenLastAccessUpdateFailure_whenFilter_thenAuthenticateUser()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + TOKEN);

        when(redisBlacklistService.isBlacklisted(TOKEN))
                .thenReturn(false);

        when(jwtProvider.extractUserId(TOKEN))
                .thenReturn(USER_ID);

        when(jwtProvider.extractSessionId(TOKEN))
                .thenReturn(SESSION_ID);

        when(sessionBlockStore.isBlocked(SESSION_ID))
                .thenReturn(false);

        when(jwtProvider.extractUserRole(TOKEN))
                .thenReturn(USER_ROLE);

        when(redisSessionService.existsSession(SESSION_ID))
                .thenReturn(true);

        doThrow(new RuntimeException("Redis connection failed"))
                .when(redisSessionService)
                .updateLastAccess(SESSION_ID);

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);

        assertTrue(
                authentication.getPrincipal() instanceof UserPrincipal
        );

        verify(redisSessionService)
                .updateLastAccess(SESSION_ID);

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 검증 실패 시 인증 거부")
    void givenInvalidToken_whenFilter_thenRejectAuthentication()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + TOKEN);

        when(redisBlacklistService.isBlacklisted(TOKEN))
                .thenReturn(false);

        doThrow(new BaseException(ErrorEnum.TOKEN_INVALID))
                .when(jwtProvider)
                .validateToken(TOKEN);

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(jwtProvider)
                .validateToken(TOKEN);

        verify(jwtProvider, never())
                .extractUserId(anyString());

        verify(response)
                .setStatus(ErrorEnum.TOKEN_INVALID.getStatus());

        verify(filterChain, never())
                .doFilter(any(), any());
    }

    @Test
    @DisplayName("JWT 인증 실패 시 Error 응답 반환")
    void givenBaseException_whenFilter_thenSendErrorResponse()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + TOKEN);

        when(redisBlacklistService.isBlacklisted(TOKEN))
                .thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        verify(response)
                .setStatus(ErrorEnum.LOGOUT_TOKEN.getStatus());

        verify(response)
                .setContentType(MediaType.APPLICATION_JSON_VALUE);

        verify(response)
                .setCharacterEncoding("UTF-8");

        verify(filterChain, never())
                .doFilter(any(), any());

        verify(objectMapper).writeValue(same(responseWriter), any());
    }

    @Test
    @DisplayName("예상하지 못한 예외 발생 시 TOKEN_INVALID 응답")
    void givenUnexpectedException_whenFilter_thenSendTokenInvalidResponse()
            throws ServletException, IOException {

        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + TOKEN);

        when(redisBlacklistService.isBlacklisted(TOKEN))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // when
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(response)
                .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        verify(response)
                .setContentType(MediaType.APPLICATION_JSON_VALUE);

        verify(response)
                .setCharacterEncoding("UTF-8");

        verify(filterChain, never())
                .doFilter(any(), any());

        verify(objectMapper).writeValue(same(responseWriter), any());
    }
}