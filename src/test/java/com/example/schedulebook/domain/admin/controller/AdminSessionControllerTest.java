package com.example.schedulebook.domain.admin.controller;

import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.filter.CachedBodyFilter;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.domain.admin.service.AdminSessionService;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSessionController.class)
@Import(SecurityConfig.class)
class AdminSessionControllerTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String SESSION_ID = "session-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSessionService adminSessionService;

    // SecurityConfig 의 SecurityFilterChain 의존성
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CachedBodyFilter cachedBodyFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @BeforeEach
    void setUpSecurityFilters() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0, ServletRequest.class);
            ServletResponse response = invocation.getArgument(1, ServletResponse.class);
            FilterChain chain = invocation.getArgument(2, FilterChain.class);

            chain.doFilter(request, response);
            return null;
        }).when(cachedBodyFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0, ServletRequest.class);
            ServletResponse response = invocation.getArgument(1, ServletResponse.class);
            FilterChain chain = invocation.getArgument(2, FilterChain.class);

            chain.doFilter(request, response);
            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0, ServletRequest.class);
            ServletResponse response = invocation.getArgument(1, ServletResponse.class);
            FilterChain chain = invocation.getArgument(2, FilterChain.class);

            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response =
                    invocation.getArgument(1, HttpServletResponse.class);

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(customAuthenticationEntryPoint)
                .commence(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response =
                    invocation.getArgument(1, HttpServletResponse.class);

            response.setStatus(HttpStatus.FORBIDDEN.value());
            return null;
        }).when(customAccessDeniedHandler)
                .handle(any(), any(), any());
    }

    @Test
    void 내_세션_목록을_조회한다() throws Exception {
        // given
        SessionInfoResponse session1 = mock(SessionInfoResponse.class);
        SessionInfoResponse session2 = mock(SessionInfoResponse.class);

        when(adminSessionService.findAllUserSessions(ADMIN_ID, USER_ID))
                .thenReturn(List.of(session1, session2));

        // when & then
        mockMvc.perform(
                        get("/admin/users/{userId}/sessions", USER_ID)
                                .with(authenticatedAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(adminSessionService)
                .findAllUserSessions(ADMIN_ID, USER_ID);
    }

    @Test
    void 세션이_없으면_빈_목록을_반환한다() throws Exception {
        // given
        when(adminSessionService.findAllUserSessions(ADMIN_ID, USER_ID))
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(
                        get("/admin/users/{userId}/sessions", USER_ID)
                                .with(authenticatedAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(adminSessionService)
                .findAllUserSessions(ADMIN_ID, USER_ID);
    }

    @Test
    void 특정_사용자의_특정_세션을_로그아웃한다() throws Exception {
        // when & then
        mockMvc.perform(
                        delete(
                                "/admin/users/{userId}/sessions/{sessionId}",
                                USER_ID,
                                SESSION_ID
                        )
                                .with(authenticatedAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<HttpServletRequest> requestCaptor =
                ArgumentCaptor.forClass(HttpServletRequest.class);

        verify(adminSessionService).logoutUserOneSession(
                eq(ADMIN_ID),
                eq(USER_ID),
                eq(SESSION_ID),
                requestCaptor.capture()
        );

        assertThat(requestCaptor.getValue()).isNotNull();
    }

    @Test
    void 특정_사용자의_모든_세션을_로그아웃한다() throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/admin/users/{userId}/sessions", USER_ID)
                                .with(authenticatedAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<HttpServletRequest> requestCaptor =
                ArgumentCaptor.forClass(HttpServletRequest.class);

        verify(adminSessionService).logoutUserAllSession(
                eq(ADMIN_ID),
                eq(USER_ID),
                requestCaptor.capture()
        );

        assertThat(requestCaptor.getValue()).isNotNull();
    }

    @Test
    void 인증되지_않은_사용자는_관리자_세션_조회_API에_접근할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/admin/users/{userId}/sessions", USER_ID)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminSessionService);
    }

    @Test
    void 일반_사용자는_관리자_세션_조회_API에_접근할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/admin/users/{userId}/sessions", USER_ID)
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminSessionService);
    }

    @Test
    void 일반_사용자는_특정_세션_강제_로그아웃_API에_접근할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(
                        delete(
                                "/admin/users/{userId}/sessions/{sessionId}",
                                USER_ID,
                                SESSION_ID
                        )
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminSessionService);
    }

    @Test
    void 일반_사용자는_모든_세션_강제_로그아웃_API에_접근할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/admin/users/{userId}/sessions", USER_ID)
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminSessionService);
    }

    @Test
    void 인증되지_않은_사용자는_특정_세션_강제_로그아웃_API에_접근할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(
                        delete(
                                "/admin/users/{userId}/sessions/{sessionId}",
                                USER_ID,
                                SESSION_ID
                        )
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminSessionService);
    }

    @Test
    void 인증되지_않은_사용자는_모든_세션_강제_로그아웃_API에_접근할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/admin/users/{userId}/sessions", USER_ID)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminSessionService);
    }

    private RequestPostProcessor authenticatedAdmin() {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        ADMIN_ID,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );
    }

    private RequestPostProcessor authenticatedUser() {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        USER_ID,
                        null,
                        List.of()
                )
        );
    }
}