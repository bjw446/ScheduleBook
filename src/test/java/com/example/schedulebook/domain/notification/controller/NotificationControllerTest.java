package com.example.schedulebook.domain.notification.controller;

import com.example.schedulebook.common.config.GlobalExceptionHandler;
import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.common.security.UserPrincipal;
import com.example.schedulebook.domain.notification.dto.response.NotificationDetailResponse;
import com.example.schedulebook.domain.notification.dto.response.NotificationSummaryResponse;
import com.example.schedulebook.domain.notification.dto.response.UnreadNotificationCountResponse;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.user.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class
})
class NotificationControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 10L;
    private static final Long TARGET_ID = 20L;
    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 10, 8, 9, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

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

    @Test
    void getAllMyNotification_인증된_사용자이면_본인의_알림_목록을_반환한다() throws Exception {
        // given
        NotificationSummaryResponse notification =
                new NotificationSummaryResponse(
                        NOTIFICATION_ID,
                        "알림 제목",
                        NotificationType.SCHEDULE_COMMENT,
                        false,
                        CREATED_AT
                );

        when(notificationService.findAllMyNotification(USER_ID))
                .thenReturn(List.of(notification));

        // when & then
        mockMvc.perform(
                        get("/notifications")
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].notificationId").value(NOTIFICATION_ID))
                .andExpect(jsonPath("$.data[0].title").value("알림 제목"))
                .andExpect(jsonPath("$.data[0].isRead").value(false))
                .andExpect(jsonPath("$.data[0].createdAt").exists());

        verify(notificationService)
                .findAllMyNotification(USER_ID);
    }

    @Test
    void getOneMyNotification_인증된_사용자이면_본인의_알림_상세_정보를_반환한다() throws Exception {
        // given
        NotificationDetailResponse response =
                new NotificationDetailResponse(
                        NOTIFICATION_ID,
                        "알림 제목",
                        "알림 내용",
                        NotificationType.SCHEDULE_COMMENT,
                        false,
                        TARGET_ID,
                        CREATED_AT
                );

        when(notificationService.findOneMyNotification(NOTIFICATION_ID, USER_ID))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        get("/notifications/{notificationId}", NOTIFICATION_ID)
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.notificationId").value(NOTIFICATION_ID))
                .andExpect(jsonPath("$.data.title").value("알림 제목"))
                .andExpect(jsonPath("$.data.content").value("알림 내용"))
                .andExpect(jsonPath("$.data.isRead").value(false))
                .andExpect(jsonPath("$.data.targetId").value(TARGET_ID))
                .andExpect(jsonPath("$.data.createdAt").exists());

        verify(notificationService)
                .findOneMyNotification(NOTIFICATION_ID, USER_ID);
    }

    @Test
    void getUnreadCount_인증된_사용자이면_읽지_않은_알림_개수를_반환한다() throws Exception {
        // given
        UnreadNotificationCountResponse response =
                new UnreadNotificationCountResponse(3L);

        when(notificationService.getUnreadCount(USER_ID))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        get("/notifications/unread-count")
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.count").value(3));

        verify(notificationService)
                .getUnreadCount(USER_ID);
    }

    @Test
    void readNotification_인증된_사용자이면_본인의_알림을_읽음_처리한다() throws Exception {
        // when & then
        mockMvc.perform(
                        patch("/notifications/{notificationId}/read", NOTIFICATION_ID)
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(notificationService)
                .readNotification(NOTIFICATION_ID, USER_ID);
    }

    @Test
    void readAllNotifications_인증된_사용자이면_본인의_알림을_모두_읽음_처리한다() throws Exception {
        // when & then
        mockMvc.perform(
                        patch("/notifications/read-all")
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(notificationService)
                .readAllNotifications(USER_ID);
    }

    @Test
    void getAllMyNotification_인증되지_않은_사용자이면_401과_공통_에러_응답을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/notifications")
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.UNAUTHORIZED.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.UNAUTHORIZED.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(notificationService);
    }

    private RequestPostProcessor authenticated() {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(USER_ID, UserRole.USER),
                        null,
                        List.of()
                )
        );
    }
}