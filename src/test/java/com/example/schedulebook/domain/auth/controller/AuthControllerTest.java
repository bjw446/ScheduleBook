package com.example.schedulebook.domain.auth.controller;

import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.filter.CachedBodyFilter;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.domain.auth.dto.request.LoginRequest;
import com.example.schedulebook.domain.auth.dto.request.RefreshRequest;
import com.example.schedulebook.domain.auth.dto.request.SignupRequest;
import com.example.schedulebook.domain.auth.dto.response.LoginResponse;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.dto.response.SignupResponse;
import com.example.schedulebook.domain.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

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
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );
            return null;
        }).when(cachedBodyFilter)
                .doFilter(
                        any(ServletRequest.class),
                        any(ServletResponse.class),
                        any(FilterChain.class)
                );

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );
            return null;
        }).when(rateLimitFilter)
                .doFilter(
                        any(ServletRequest.class),
                        any(ServletResponse.class),
                        any(FilterChain.class)
                );

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(
                        any(ServletRequest.class),
                        any(ServletResponse.class),
                        any(FilterChain.class)
                );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken authenticatedUser(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of()
        );
    }

    @Test
    void 회원가입_성공() throws Exception {
        SignupRequest request = new SignupRequest(
                "test123",
                "Password1!",
                "테스트유저",
                "test@example.com",
                "010-1234-5678"
        );

        SignupResponse response = new SignupResponse(
                "test123",
                "테스트유저",
                "test@example.com",
                "010-1234-5678"
        );

        given(authService.signup(any(SignupRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 회원가입_유효하지_않은_요청이면_400() throws Exception {
        SignupRequest request = new SignupRequest(
                "",
                "short",
                "",
                "invalid-email",
                "invalid"
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 로그인_성공() throws Exception {
        LoginRequest request = new LoginRequest(
                "test123",
                "Password1!",
                null
        );

        LoginResponse response = new LoginResponse(
                1L,
                "테스트유저",
                1,
                "access-token",
                "refresh-token"
        );

        given(authService.login(any(LoginRequest.class), any()))
                .willReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 로그인_유효하지_않은_요청이면_400() throws Exception {
        LoginRequest request = new LoginRequest(
                "",
                "short",
                null
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 로그아웃_성공() throws Exception {
        String accessToken = "access-token";

        willDoNothing().given(authService)
                .logout(anyString(), any());

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 로그아웃_유효하지_않은_인증헤더이면_토큰누락_예외() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Basic access-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그아웃_Authorization_헤더가_없으면_500() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void 토큰_재발급_성공() throws Exception {
        RefreshRequest request = new RefreshRequest("refresh-token");

        LoginResponse response = new LoginResponse(
                1L,
                "테스트유저",
                1,
                "new-access-token",
                "new-refresh-token"
        );

        given(authService.refresh(any(RefreshRequest.class), any()))
                .willReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 토큰_재발급_유효하지_않은_요청이면_400() throws Exception {
        RefreshRequest request = new RefreshRequest("");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 내_세션_조회_성공() throws Exception {
        Long userId = 1L;

        List<SessionInfoResponse> responses = List.of(
                new SessionInfoResponse(
                        "session-1",
                        "127.0.0.1",
                        "Chrome",
                        null,
                        null
                )
        );

        given(authService.findMySessions(userId))
                .willReturn(responses);

        mockMvc.perform(get("/auth/sessions")
                        .with(authentication(authenticatedUser(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sessionId")
                        .value("session-1"));
    }

    @Test
    void 내_세션_조회_인증정보가_없으면_401() throws Exception {
        mockMvc.perform(get("/auth/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 특정_세션_로그아웃_성공() throws Exception {
        Long userId = 1L;
        String sessionId = "session-1";

        willDoNothing().given(authService)
                .logoutSession(eq(userId), eq(sessionId), any());

        mockMvc.perform(delete(
                        "/auth/sessions/{sessionId}",
                        sessionId
                )
                        .with(authentication(authenticatedUser(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 특정_세션_로그아웃_인증정보가_없으면_401() throws Exception {
        mockMvc.perform(delete(
                        "/auth/sessions/{sessionId}",
                        "session-1"
                ))
                .andExpect(status().isUnauthorized());
    }
}