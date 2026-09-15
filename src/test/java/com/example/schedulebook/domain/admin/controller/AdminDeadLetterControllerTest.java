package com.example.schedulebook.domain.admin.controller;

import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.filter.CachedBodyFilter;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterDetailResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterSummaryResponse;
import com.example.schedulebook.domain.admin.service.AdminDeadLetterService;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDeadLetterController.class)
@Import(SecurityConfig.class)
class AdminDeadLetterControllerTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long DEAD_LETTER_ID = 100L;
    private static final Long USER_ID = 200L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDeadLetterService adminDeadLetterService;

    /*
     * SecurityConfig에서 사용하는 필터/보안 컴포넌트 Mock
     */
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
            ServletRequest request = invocation.getArgument(
                    0,
                    ServletRequest.class
            );

            ServletResponse response = invocation.getArgument(
                    1,
                    ServletResponse.class
            );

            FilterChain chain = invocation.getArgument(
                    2,
                    FilterChain.class
            );

            chain.doFilter(request, response);

            return null;
        }).when(cachedBodyFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(
                    0,
                    ServletRequest.class
            );

            ServletResponse response = invocation.getArgument(
                    1,
                    ServletResponse.class
            );

            FilterChain chain = invocation.getArgument(
                    2,
                    FilterChain.class
            );

            chain.doFilter(request, response);

            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(
                    0,
                    ServletRequest.class
            );

            ServletResponse response = invocation.getArgument(
                    1,
                    ServletResponse.class
            );

            FilterChain chain = invocation.getArgument(
                    2,
                    FilterChain.class
            );

            chain.doFilter(request, response);

            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        /*
         * Spring Security가 사용하는 Response는
         * HeaderWriterResponse 등의 wrapper일 수 있으므로
         * MockHttpServletResponse로 캐스팅하면 안 된다.
         */
        doAnswer(invocation -> {
            HttpServletResponse response =
                    invocation.getArgument(
                            1,
                            jakarta.servlet.http.HttpServletResponse.class
                    );

            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            return null;
        }).when(customAuthenticationEntryPoint)
                .commence(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response =
                    invocation.getArgument(
                            1,
                            jakarta.servlet.http.HttpServletResponse.class
                    );

            response.setStatus(HttpStatus.FORBIDDEN.value());

            return null;
        }).when(customAccessDeniedHandler)
                .handle(any(), any(), any());
    }

    @Test
    void DeadLetter를_페이지로_조회하면_페이지_정보와_목록을_반환한다() throws Exception {

        LocalDateTime failedAt =
                LocalDateTime.of(2026, 9, 15, 10, 30);

        DeadLetterSummaryResponse response =
                new DeadLetterSummaryResponse(
                        DEAD_LETTER_ID,
                        DeadLetterType.FORCE_LOGOUT,
                        DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER,
                        DeadLetterAggregateType.SESSION,
                        DeadLetterStatus.PENDING,
                        "session-1",
                        failedAt
                );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        PageResponse<DeadLetterSummaryResponse> pageResponse =
                PageResponse.register(
                        new PageImpl<>(
                                List.of(response),
                                pageable,
                                1
                        )
                );

        when(adminDeadLetterService.findAllDeadLetters(
                eq(ADMIN_ID),
                any(Pageable.class)
        )).thenReturn(pageResponse);

        mockMvc.perform(
                        get("/admin/dead-letters")
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

                // PageResponse content 검증
                .andExpect(jsonPath("$.data.content[0].deadLetterId")
                        .value(DEAD_LETTER_ID))
                .andExpect(jsonPath("$.data.content[0].deadLetterType")
                        .value(DeadLetterType.FORCE_LOGOUT.name()))
                .andExpect(jsonPath("$.data.content[0].deadLetterSource")
                        .value(
                                DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER.name()
                        ))
                .andExpect(jsonPath(
                        "$.data.content[0].deadLetterAggregateType"
                ).value(
                        DeadLetterAggregateType.SESSION.name()
                ))
                .andExpect(jsonPath("$.data.content[0].deadLetterStatus")
                        .value(DeadLetterStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.content[0].aggregateId")
                        .value("session-1"))
                .andExpect(jsonPath("$.data.content[0].failedAt")
                        .value("2026-09-15T10:30:00"));

        verify(adminDeadLetterService).findAllDeadLetters(
                eq(ADMIN_ID),
                any(Pageable.class)
        );
    }

    @Test
    void DeadLetter_단건을_조회하면_상세_정보를_반환한다() throws Exception {

        LocalDateTime failedAt =
                LocalDateTime.of(2026, 9, 15, 10, 30);

        DeadLetterDetailResponse response =
                new DeadLetterDetailResponse(
                        DEAD_LETTER_ID,
                        DeadLetterType.FORCE_LOGOUT,
                        DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER,
                        DeadLetterAggregateType.SESSION,
                        DeadLetterStatus.PENDING,
                        "session-1",
                        USER_ID,
                        "payload",
                        "RuntimeException",
                        "notification failed",
                        3,
                        failedAt
                );

        when(adminDeadLetterService.findOneDeadLetter(
                ADMIN_ID,
                DEAD_LETTER_ID
        )).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/admin/dead-letters/{deadLetterId}",
                                DEAD_LETTER_ID
                        )
                                .with(authenticatedAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deadLetterId")
                        .value(DEAD_LETTER_ID))
                .andExpect(jsonPath("$.data.deadLetterType")
                        .value(DeadLetterType.FORCE_LOGOUT.name()))
                .andExpect(jsonPath("$.data.deadLetterSource")
                        .value(
                                DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER.name()
                        ))
                .andExpect(jsonPath("$.data.deadLetterAggregateType")
                        .value(
                                DeadLetterAggregateType.SESSION.name()
                        ))
                .andExpect(jsonPath("$.data.deadLetterStatus")
                        .value(DeadLetterStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.aggregateId")
                        .value("session-1"))
                .andExpect(jsonPath("$.data.userId")
                        .value(USER_ID))
                .andExpect(jsonPath("$.data.payload")
                        .value("payload"))
                .andExpect(jsonPath("$.data.exceptionType")
                        .value("RuntimeException"))
                .andExpect(jsonPath("$.data.reason")
                        .value("notification failed"))
                .andExpect(jsonPath("$.data.retryCount")
                        .value(3))
                .andExpect(jsonPath("$.data.failedAt")
                        .value("2026-09-15T10:30:00"));

        verify(adminDeadLetterService)
                .findOneDeadLetter(
                        ADMIN_ID,
                        DEAD_LETTER_ID
                );
    }

    @Test
    void DeadLetter를_복구하면_서비스를_호출하고_성공_응답을_반환한다()
            throws Exception {

        doNothing().when(adminDeadLetterService)
                .recoverDeadLetter(
                        ADMIN_ID,
                        DEAD_LETTER_ID
                );

        mockMvc.perform(
                        patch(
                                "/admin/dead-letters/{deadLetterId}/recover",
                                DEAD_LETTER_ID
                        )
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

        verify(adminDeadLetterService)
                .recoverDeadLetter(
                        ADMIN_ID,
                        DEAD_LETTER_ID
                );
    }

    @Test
    void 인증되지_않은_사용자는_DeadLetter를_조회할_수_없다()
            throws Exception {

        mockMvc.perform(
                        get("/admin/dead-letters")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminDeadLetterService);
    }

    @Test
    void 일반_사용자는_DeadLetter를_조회할_수_없다()
            throws Exception {

        mockMvc.perform(
                        get("/admin/dead-letters")
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminDeadLetterService);
    }

    @Test
    void 인증된_사용자는_DeadLetter_단건을_조회할_수_없다()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/admin/dead-letters/{deadLetterId}",
                                DEAD_LETTER_ID
                        )
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminDeadLetterService);
    }

    @Test
    void 인증된_사용자는_DeadLetter를_복구할_수_없다()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/admin/dead-letters/{deadLetterId}/recover",
                                DEAD_LETTER_ID
                        )
                                .with(authenticatedUser())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminDeadLetterService);
    }

    /**
     * SUPER_ADMIN 권한을 가진 관리자 인증
     */
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

    /**
     * 인증은 되었지만 SUPER_ADMIN 권한이 없는 일반 사용자 인증
     */
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