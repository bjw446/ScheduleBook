package com.example.schedulebook.domain.friend.controller;

import com.example.schedulebook.common.config.GlobalExceptionHandler;
import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.friend.dto.request.FriendRequest;
import com.example.schedulebook.domain.friend.dto.response.FriendResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendSummaryResponse;
import com.example.schedulebook.domain.friend.dto.response.ReceivedFriendRequestResponse;
import com.example.schedulebook.domain.friend.dto.response.SentFriendRequestResponse;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.service.FriendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(FriendController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser(username = "1", roles = "USER")
class FriendControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long FRIEND_ID = 10L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long REQUESTER_ID = 3L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FriendService friendService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    private MockMvc mockMvc;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock
                .when(SecurityUtils::getCurrentUserId)
                .thenReturn(USER_ID);

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(request, response);

            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(request, response);

            return null;
        }).when(rateLimitFilter)
                .doFilter(any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void 친구_요청에_성공하면_201과_생성된_친구_정보를_반환한다()
            throws Exception {
        // given
        FriendRequest request = new FriendRequest(RECEIVER_ID);

        FriendResponse response = new FriendResponse(
                FRIEND_ID,
                USER_ID,
                RECEIVER_ID,
                FriendStatus.PENDING
        );

        when(friendService.requestFriend(
                eq(request),
                eq(USER_ID)
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        post("/friends/request")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.CREATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.friendId").value(FRIEND_ID))
                .andExpect(jsonPath("$.data.requesterId").value(USER_ID))
                .andExpect(jsonPath("$.data.receiverId").value(RECEIVER_ID))
                .andExpect(jsonPath("$.data.friendStatus")
                        .value(FriendStatus.PENDING.name()));

        verify(friendService)
                .requestFriend(eq(request), eq(USER_ID));
    }

    @Test
    void 친구_요청시_receiverId가_null이면_400과_Validation_응답을_반환한다()
            throws Exception {
        // given
        FriendRequest request = new FriendRequest(null);

        // when & then
        mockMvc.perform(
                        post("/friends/request")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_ARGUMENT.getStatus()));

        verifyNoInteractions(friendService);
    }

    @Test
    void 친구_요청시_receiverId가_0이면_400과_Validation_응답을_반환한다()
            throws Exception {
        // given
        FriendRequest request = new FriendRequest(0L);

        // when & then
        mockMvc.perform(
                        post("/friends/request")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_ARGUMENT.getStatus()));

        verifyNoInteractions(friendService);
    }

    @Test
    void 전체_친구_조회에_성공하면_200과_친구_목록을_반환한다()
            throws Exception {
        // given
        FriendSummaryResponse response = new FriendSummaryResponse(
                FRIEND_ID,
                REQUESTER_ID,
                "친구",
                10,
                true
        );

        when(friendService.findAllFriends(USER_ID))
                .thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].friendId").value(FRIEND_ID))
                .andExpect(jsonPath("$.data[0].userId").value(REQUESTER_ID))
                .andExpect(jsonPath("$.data[0].nickname").value("친구"))
                .andExpect(jsonPath("$.data[0].level").value(10))
                .andExpect(jsonPath("$.data[0].online").value(true));

        verify(friendService)
                .findAllFriends(USER_ID);
    }

    @Test
    void 받은_친구_요청_조회에_성공하면_200과_요청_목록을_반환한다()
            throws Exception {
        // given
        ReceivedFriendRequestResponse response =
                new ReceivedFriendRequestResponse(
                        FRIEND_ID,
                        REQUESTER_ID,
                        "친구요청자"
                );

        when(friendService.findReceivedRequests(USER_ID))
                .thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/friends/requests/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].friendId").value(FRIEND_ID))
                .andExpect(jsonPath("$.data[0].requesterId").value(REQUESTER_ID))
                .andExpect(jsonPath("$.data[0].nickname").value("친구요청자"));

        verify(friendService)
                .findReceivedRequests(USER_ID);
    }

    @Test
    void 보낸_친구_요청_조회에_성공하면_200과_요청_목록을_반환한다()
            throws Exception {
        // given
        SentFriendRequestResponse response =
                new SentFriendRequestResponse(
                        FRIEND_ID,
                        RECEIVER_ID,
                        "친구수신자"
                );

        when(friendService.findSentRequests(USER_ID))
                .thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/friends/requests/sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].friendId").value(FRIEND_ID))
                .andExpect(jsonPath("$.data[0].receiverId").value(RECEIVER_ID))
                .andExpect(jsonPath("$.data[0].nickname").value("친구수신자"));

        verify(friendService)
                .findSentRequests(USER_ID);
    }

    @Test
    void 친구_요청을_수락하면_200과_수락된_친구_정보를_반환한다()
            throws Exception {
        // given
        FriendResponse response = new FriendResponse(
                FRIEND_ID,
                REQUESTER_ID,
                USER_ID,
                FriendStatus.ACCEPTED
        );

        when(friendService.acceptFriend(FRIEND_ID, USER_ID))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        patch("/friends/{friendId}/accept", FRIEND_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.friendId").value(FRIEND_ID))
                .andExpect(jsonPath("$.data.requesterId").value(REQUESTER_ID))
                .andExpect(jsonPath("$.data.receiverId").value(USER_ID))
                .andExpect(jsonPath("$.data.friendStatus")
                        .value(FriendStatus.ACCEPTED.name()));

        verify(friendService)
                .acceptFriend(FRIEND_ID, USER_ID);
    }

    @Test
    void 친구_요청을_거절하면_200과_업데이트_성공_응답을_반환한다()
            throws Exception {
        // when & then
        mockMvc.perform(
                        patch("/friends/{friendId}/reject", FRIEND_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(friendService)
                .rejectFriend(FRIEND_ID, USER_ID);
    }

    @Test
    void 친구를_차단하면_200과_업데이트_성공_응답을_반환한다()
            throws Exception {
        // when & then
        mockMvc.perform(
                        patch("/friends/{friendId}/block", FRIEND_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(friendService)
                .blockFriend(FRIEND_ID, USER_ID);
    }

    @Test
    void 친구를_삭제하면_200과_삭제_성공_응답을_반환한다()
            throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/friends/{friendId}", FRIEND_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.DELETE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.DELETE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(friendService)
                .deleteFriend(FRIEND_ID, USER_ID);
    }
}