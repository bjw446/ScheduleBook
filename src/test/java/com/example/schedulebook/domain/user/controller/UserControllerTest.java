package com.example.schedulebook.domain.user.controller;

import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.filter.CachedBodyFilter;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.domain.user.dto.request.UpdateUserPasswordRequest;
import com.example.schedulebook.domain.user.dto.request.UpdateUserRequest;
import com.example.schedulebook.domain.user.dto.request.WithdrawUserRequest;
import com.example.schedulebook.domain.user.dto.response.UpdateUserResponse;
import com.example.schedulebook.domain.user.dto.response.UserResponse;
import com.example.schedulebook.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CachedBodyFilter cachedBodyFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @MockitoBean
    private JwtProperties jwtProperties;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
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
                .doFilter(any(), any(), any());

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;
        }).when(rateLimitFilter)
                .doFilter(any(), any(), any());

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());

        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response =
                    invocation.getArgument(1);

            response.setStatus(401);

            return null;
        }).when(customAuthenticationEntryPoint)
                .commence(any(), any(), any());

        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response =
                    invocation.getArgument(1);

            response.setStatus(403);

            return null;
        }).when(customAccessDeniedHandler)
                .handle(any(), any(), any());
    }

    @Test
    void 내_프로필을_정상적으로_조회한다() throws Exception {
        // given
        UserResponse response = new UserResponse(
                "테스트유저",
                3,
                150,
                300,
                10,
                5,
                20
        );

        given(userService.findMyProfile(USER_ID))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/users/me")
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.data.level").value(3))
                .andExpect(jsonPath("$.data.exp").value(150));

        verify(userService)
                .findMyProfile(USER_ID);

    }

    @Test
    void 프로필_수정에_성공한다() throws Exception {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                "새닉네임",
                "new@example.com",
                "010-9999-8888"
        );

        UpdateUserResponse response = new UpdateUserResponse(
                "새닉네임",
                "new@example.com",
                "010-9999-8888"
        );

        given(userService.updateMyProfile(request, USER_ID))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        put("/users/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-9999-8888"));

        verify(userService)
                .updateMyProfile(request, USER_ID);
    }

    @Test
    void 비밀번호_변경에_성공한다() throws Exception {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(
                "Password1!",
                "NewPassword1!"
        );

        // when & then
        mockMvc.perform(
                        put("/users/me/password")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                                .with(authenticated())
                )
                .andExpect(status().isOk());

        verify(userService)
                .updateMyPassword(request, USER_ID);
    }

    @Test
    void 회원탈퇴에_성공한다() throws Exception {
        // given
        WithdrawUserRequest request = new WithdrawUserRequest(
                "Password1!"
        );

        // when & then
        mockMvc.perform(
                        delete("/users/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                                .with(authenticated())
                )
                .andExpect(status().isOk());

        verify(userService)
                .withdraw(request, USER_ID);
    }

    @Test
    void 인증되지_않은_사용자가_프로필을_조회하면_401을_반환한다() throws Exception {
        mockMvc.perform(
                        get("/users/me")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증되지_않은_사용자가_프로필을_수정하면_401을_반환한다() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "새닉네임",
                "new@example.com",
                "010-9999-8888"
        );

        mockMvc.perform(
                        put("/users/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증되지_않은_사용자가_비밀번호를_변경하면_401을_반환한다() throws Exception {
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(
                "Password1!",
                "NewPassword1!"
        );

        mockMvc.perform(
                        put("/users/me/password")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증되지_않은_사용자가_회원탈퇴를_요청하면_401을_반환한다() throws Exception {
        WithdrawUserRequest request = new WithdrawUserRequest(
                "Password1!"
        );

        mockMvc.perform(
                        delete("/users/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 권한이_없는_인증_사용자가_관리자_페이지에_접근하면_403을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/admin/test")
                                .with(authenticated())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void 유효하지_않은_프로필_수정_요청이면_400을_반환하고_서비스를_호출하지_않는다() throws Exception {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                "",
                "new@example.com",
                "010-9999-8888"
        );

        // when & then
        mockMvc.perform(
                        put("/users/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                                .with(authenticated())
                )
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .updateMyProfile(any(), any());
    }

    @Test
    void 유효하지_않은_비밀번호_변경_요청이면_400을_반환하고_서비스를_호출하지_않는다() throws Exception {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(
                "",
                ""
        );

        // when & then
        mockMvc.perform(
                        put("/users/me/password")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                                .with(authenticated())
                )
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .updateMyPassword(any(), any());
    }

    @Test
    void 유효하지_않은_회원탈퇴_요청이면_400을_반환하고_서비스를_호출하지_않는다() throws Exception {
        // given
        WithdrawUserRequest request = new WithdrawUserRequest("");

        // when & then
        mockMvc.perform(
                        delete("/users/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                                .with(authenticated())
                )
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .withdraw(any(), any());
    }

    private RequestPostProcessor authenticated() {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        USER_ID,
                        null,
                        List.of()
                )
        );
    }
}