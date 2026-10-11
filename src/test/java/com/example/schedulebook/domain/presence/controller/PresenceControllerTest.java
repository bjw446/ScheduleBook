package com.example.schedulebook.domain.presence.controller;

import com.example.schedulebook.common.config.GlobalExceptionHandler;
import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.common.security.UserPrincipal;
import com.example.schedulebook.domain.presence.dto.response.UserPresenceResponse;
import com.example.schedulebook.domain.presence.service.PresenceService;
import com.example.schedulebook.domain.user.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PresenceController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class
})
@DisplayName("PresenceController 테스트")
class PresenceControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PresenceService presenceService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(USER_ID, UserRole.USER),
                        null,
                        List.of()
                )
        );

        doAnswer(invocation -> {
            FilterChain filterChain =
                    invocation.getArgument(2, FilterChain.class);

            filterChain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            FilterChain filterChain =
                    invocation.getArgument(2, FilterChain.class);

            filterChain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("접속 상태 조회에 성공하면 200 OK와 접속 상태를 반환한다")
    void getPresence_성공하면_200_OK와_접속_상태를_반환한다() throws Exception {
        // given
        UserPresenceResponse response =
                new UserPresenceResponse(TARGET_USER_ID, true, 2);

        when(presenceService.findPresence(USER_ID, TARGET_USER_ID))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        get("/presences/{userId}", TARGET_USER_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userId").value(TARGET_USER_ID))
                .andExpect(jsonPath("$.data.online").value(true))
                .andExpect(jsonPath("$.data.activeSessions").value(2))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(presenceService).findPresence(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("접속 상태 조회 결과가 오프라인이면 오프라인 상태를 반환한다")
    void getPresence_오프라인이면_오프라인_상태를_반환한다() throws Exception {
        // given
        UserPresenceResponse response =
                new UserPresenceResponse(TARGET_USER_ID, false, 0);

        when(presenceService.findPresence(USER_ID, TARGET_USER_ID))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        get("/presences/{userId}", TARGET_USER_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(TARGET_USER_ID))
                .andExpect(jsonPath("$.data.online").value(false))
                .andExpect(jsonPath("$.data.activeSessions").value(0));

        verify(presenceService).findPresence(USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("요청한 사용자 ID를 PresenceService에 전달한다")
    void getPresence_인증된_사용자와_대상_사용자_ID를_Service에_전달한다() throws Exception {
        // given
        when(presenceService.findPresence(USER_ID, TARGET_USER_ID))
                .thenReturn(new UserPresenceResponse(TARGET_USER_ID, true, 1));

        // when
        mockMvc.perform(
                        get("/presences/{userId}", TARGET_USER_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk());

        // then
        verify(presenceService).findPresence(USER_ID, TARGET_USER_ID);
        verifyNoMoreInteractions(presenceService);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 접속 상태를 조회할 수 없다")
    void getPresence_인증되지_않은_사용자는_접속_상태를_조회할_수_없다() throws Exception {
        // given
        SecurityContextHolder.clearContext();

        // when & then
        mockMvc.perform(
                        get("/presences/{userId}", TARGET_USER_ID)
                                .with(anonymous())
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.UNAUTHORIZED.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorEnum.UNAUTHORIZED.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(presenceService);
    }
}