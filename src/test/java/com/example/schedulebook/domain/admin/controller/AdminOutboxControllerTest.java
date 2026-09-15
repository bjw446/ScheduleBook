package com.example.schedulebook.domain.admin.controller;

import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.filter.CachedBodyFilter;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.domain.admin.dto.response.CleanupOutboxResponse;
import com.example.schedulebook.domain.admin.dto.response.OutboxResponse;
import com.example.schedulebook.domain.admin.dto.response.OutboxStatsResponse;
import com.example.schedulebook.domain.admin.service.AdminOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOutboxController.class)
@Import(SecurityConfig.class)
class AdminOutboxControllerTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long OUTBOX_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminOutboxService adminOutboxService;

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
    void setUp() throws ServletException, IOException {

        doAnswer(invocation -> {
            ServletRequest request =
                    invocation.getArgument(0, ServletRequest.class);

            ServletResponse response =
                    invocation.getArgument(1, ServletResponse.class);

            FilterChain chain =
                    invocation.getArgument(2, FilterChain.class);

            chain.doFilter(request, response);

            return null;
        }).when(cachedBodyFilter)
                .doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request =
                    invocation.getArgument(0, ServletRequest.class);

            ServletResponse response =
                    invocation.getArgument(1, ServletResponse.class);

            FilterChain chain =
                    invocation.getArgument(2, FilterChain.class);

            chain.doFilter(request, response);

            return null;
        }).when(rateLimitFilter)
                .doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request =
                    invocation.getArgument(0, ServletRequest.class);

            ServletResponse response =
                    invocation.getArgument(1, ServletResponse.class);

            FilterChain chain =
                    invocation.getArgument(2, FilterChain.class);

            chain.doFilter(request, response);

            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response =
                    invocation.getArgument(
                            1,
                            HttpServletResponse.class
                    );

            response.setStatus(
                    HttpStatus.UNAUTHORIZED.value()
            );

            return null;
        }).when(customAuthenticationEntryPoint)
                .commence(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response =
                    invocation.getArgument(
                            1,
                            HttpServletResponse.class
                    );

            response.setStatus(
                    HttpStatus.FORBIDDEN.value()
            );

            return null;
        }).when(customAccessDeniedHandler)
                .handle(any(), any(), any());
    }

    @Test
    void Dead_Outbox를_페이지로_조회하면_페이지_정보와_목록을_반환한다()
            throws Exception {

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 9, 15, 10, 0);

        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 9, 15, 11, 0);

        OutboxResponse response =
                new OutboxResponse(
                        OUTBOX_ID,
                        com.example.schedulebook.domain.outbox.enums.OutboxAggregateType.USER,
                        "1",
                        com.example.schedulebook.domain.outbox.enums.OutboxEventType.AUDIT_EVENT,
                        com.example.schedulebook.domain.outbox.enums.OutboxStatus.DEAD,
                        "payload preview",
                        3,
                        "outbox failed",
                        createdAt,
                        updatedAt
                );

        Pageable pageable =
                PageRequest.of(
                        0,
                        10,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        PageResponse<OutboxResponse> pageResponse =
                PageResponse.register(
                        new PageImpl<>(
                                List.of(response),
                                pageable,
                                1
                        )
                );

        when(adminOutboxService.findAllDeadOutboxes(
                eq(ADMIN_ID),
                any(Pageable.class)
        )).thenReturn(pageResponse);

        mockMvc.perform(
                        get("/admin/outboxes/dead")
                                .with(authenticatedAdmin())
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "createdAt,desc")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id")
                        .value(OUTBOX_ID))
                .andExpect(jsonPath("$.data.content[0].aggregateType")
                        .value("USER"))
                .andExpect(jsonPath("$.data.content[0].aggregateId")
                        .value("1"))
                .andExpect(jsonPath("$.data.content[0].eventType")
                        .value("AUDIT_EVENT"))
                .andExpect(jsonPath("$.data.content[0].status")
                        .value("DEAD"))
                .andExpect(jsonPath("$.data.content[0].payloadPreview")
                        .value("payload preview"))
                .andExpect(jsonPath("$.data.content[0].retryCount")
                        .value(3))
                .andExpect(jsonPath("$.data.content[0].errorMessage")
                        .value("outbox failed"))
                .andExpect(jsonPath("$.data.content[0].createdAt")
                        .value("2026-09-15T10:00:00"))
                .andExpect(jsonPath("$.data.content[0].updatedAt")
                        .value("2026-09-15T11:00:00"));

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(adminOutboxService).findAllDeadOutboxes(
                eq(ADMIN_ID),
                pageableCaptor.capture()
        );

        Pageable captured =
                pageableCaptor.getValue();

        assertThat(captured.getPageNumber())
                .isEqualTo(0);

        assertThat(captured.getPageSize())
                .isEqualTo(10);

        assertThat(captured.getSort())
                .isEqualTo(
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );
    }

    @Test
    void Outbox를_재시도하면_서비스에_현재_사용자와_Outbox_ID를_전달한다()
            throws Exception {

        mockMvc.perform(
                        patch("/admin/outboxes/{outboxId}/retry", OUTBOX_ID)
                                .with(authenticatedAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(adminOutboxService)
                .retryOutbox(
                        ADMIN_ID,
                        OUTBOX_ID
                );
    }

    @Test
    void 성공한_Outbox를_삭제하면_기본_삭제_기간_30일을_전달한다()
            throws Exception {

        CleanupOutboxResponse response =
                new CleanupOutboxResponse(15);

        when(adminOutboxService.deleteSuccessOutbox(
                ADMIN_ID,
                30
        )).thenReturn(response);

        mockMvc.perform(
                        delete("/admin/outboxes/success")
                                .with(authenticatedAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount")
                        .value(15));

        verify(adminOutboxService)
                .deleteSuccessOutbox(
                        ADMIN_ID,
                        30
                );
    }

    @Test
    void 성공한_Outbox를_삭제하면_요청한_삭제_기간을_전달한다()
            throws Exception {

        CleanupOutboxResponse response =
                new CleanupOutboxResponse(7);

        when(adminOutboxService.deleteSuccessOutbox(
                ADMIN_ID,
                60
        )).thenReturn(response);

        mockMvc.perform(
                        delete("/admin/outboxes/success")
                                .with(authenticatedAdmin())
                                .param("days", "60")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount")
                        .value(7));

        verify(adminOutboxService)
                .deleteSuccessOutbox(
                        ADMIN_ID,
                        60
                );
    }

    @Test
    void 삭제_기간이_1보다_작으면_잘못된_요청으로_처리한다()
            throws Exception {

        mockMvc.perform(
                        delete("/admin/outboxes/success")
                                .with(authenticatedAdmin())
                                .param("days", "0")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminOutboxService);
    }

    @Test
    void Outbox_통계를_조회하면_통계_정보를_반환한다()
            throws Exception {

        OutboxStatsResponse response =
                new OutboxStatsResponse(
                        10L,
                        5L,
                        3L,
                        2L,
                        100L
                );

        when(adminOutboxService.getStats(ADMIN_ID))
                .thenReturn(response);

        mockMvc.perform(
                        get("/admin/outboxes/stats")
                                .with(authenticatedAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pending")
                        .value(10))
                .andExpect(jsonPath("$.data.processing")
                        .value(5))
                .andExpect(jsonPath("$.data.failed")
                        .value(3))
                .andExpect(jsonPath("$.data.dead")
                        .value(2))
                .andExpect(jsonPath("$.data.success")
                        .value(100));

        verify(adminOutboxService)
                .getStats(ADMIN_ID);
    }

    @Test
    void Failed_Outbox를_페이지로_조회하면_목록을_반환한다()
            throws Exception {

        OutboxResponse response =
                new OutboxResponse(
                        OUTBOX_ID,
                        com.example.schedulebook.domain.outbox.enums.OutboxAggregateType.USER,
                        "1",
                        com.example.schedulebook.domain.outbox.enums.OutboxEventType.AUDIT_EVENT,
                        com.example.schedulebook.domain.outbox.enums.OutboxStatus.FAILED,
                        "failed payload",
                        2,
                        "temporary failure",
                        LocalDateTime.of(2026, 9, 15, 10, 0),
                        LocalDateTime.of(2026, 9, 15, 11, 0)
                );

        Pageable pageable =
                PageRequest.of(
                        0,
                        10,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        PageResponse<OutboxResponse> pageResponse =
                PageResponse.register(
                        new PageImpl<>(
                                List.of(response),
                                pageable,
                                1
                        )
                );

        when(adminOutboxService.findAllFailedOutboxes(
                eq(ADMIN_ID),
                any(Pageable.class)
        )).thenReturn(pageResponse);

        mockMvc.perform(
                        get("/admin/outboxes/failed")
                                .with(authenticatedAdmin())
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "createdAt,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id")
                        .value(OUTBOX_ID))
                .andExpect(jsonPath("$.data.content[0].status")
                        .value("FAILED"))
                .andExpect(jsonPath("$.data.content[0].retryCount")
                        .value(2));

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(adminOutboxService).findAllFailedOutboxes(
                eq(ADMIN_ID),
                pageableCaptor.capture()
        );

        Pageable captured =
                pageableCaptor.getValue();

        assertThat(captured.getPageNumber())
                .isEqualTo(0);

        assertThat(captured.getPageSize())
                .isEqualTo(10);

        assertThat(captured.getSort())
                .isEqualTo(
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );
    }

    @Test
    void 인증되지_않은_사용자는_관리자_Outbox_API에_접근할_수_없다()
            throws Exception {

        mockMvc.perform(
                        get("/admin/outboxes/dead")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminOutboxService);
    }

    @Test
    void 일반_사용자는_Dead_Outbox를_조회할_수_없다()
            throws Exception {

        mockMvc.perform(
                        get("/admin/outboxes/dead")
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminOutboxService);
    }

    @Test
    void 일반_사용자는_Outbox를_재시도할_수_없다()
            throws Exception {

        mockMvc.perform(
                        patch("/admin/outboxes/{outboxId}/retry", OUTBOX_ID)
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminOutboxService);
    }

    @Test
    void 일반_사용자는_성공한_Outbox를_삭제할_수_없다()
            throws Exception {

        mockMvc.perform(
                        delete("/admin/outboxes/success")
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminOutboxService);
    }

    @Test
    void 일반_사용자는_Outbox_통계를_조회할_수_없다()
            throws Exception {

        mockMvc.perform(
                        get("/admin/outboxes/stats")
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminOutboxService);
    }

    @Test
    void 일반_사용자는_Failed_Outbox를_조회할_수_없다()
            throws Exception {

        mockMvc.perform(
                        get("/admin/outboxes/failed")
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminOutboxService);
    }

    private RequestPostProcessor authenticatedAdmin() {

        return authentication(
                new UsernamePasswordAuthenticationToken(
                        ADMIN_ID,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_SUPER_ADMIN"
                                )
                        )
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