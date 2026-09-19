package com.example.schedulebook.domain.scheduleshare.controller;

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
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantResponse;
import com.example.schedulebook.domain.scheduleparticipant.enums.AttendanceStatus;
import com.example.schedulebook.domain.scheduleshare.dto.request.ScheduleShareRequest;
import com.example.schedulebook.domain.scheduleshare.dto.request.UpdateAttendanceRequest;
import com.example.schedulebook.domain.scheduleshare.dto.response.OwnedShareDetailResponse;
import com.example.schedulebook.domain.scheduleshare.dto.response.OwnedShareResponse;
import com.example.schedulebook.domain.scheduleshare.dto.response.ScheduleShareResponse;
import com.example.schedulebook.domain.scheduleshare.dto.response.SharedScheduleDetailResponse;
import com.example.schedulebook.domain.scheduleshare.dto.response.SharedScheduleResponse;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleShareController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser(username = "1", roles = "USER")
class ScheduleShareControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long FRIEND_ID = 2L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long SHARE_ID = 100L;

    private static final LocalDate SCHEDULE_DATE = LocalDate.of(2026, 9, 18);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleShareService scheduleShareService;

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
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(request, response);

            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void 일정_공유에_성공하면_201과_공유정보를_반환한다() throws Exception {
        ScheduleShareRequest request = new ScheduleShareRequest(FRIEND_ID);

        ScheduleShareResponse response = new ScheduleShareResponse(
                SHARE_ID,
                SCHEDULE_ID,
                FRIEND_ID,
                "친구사용자"
        );

        when(scheduleShareService.shareSchedule(
                eq(SCHEDULE_ID),
                any(ScheduleShareRequest.class),
                eq(USER_ID)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/schedule_shares/{scheduleId}", SCHEDULE_ID)
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
                .andExpect(jsonPath("$.data.shareId").value(SHARE_ID))
                .andExpect(jsonPath("$.data.scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data.sharedUserId").value(FRIEND_ID))
                .andExpect(jsonPath("$.data.sharedUserNickname")
                        .value("친구사용자"));

        ArgumentCaptor<ScheduleShareRequest> requestCaptor =
                ArgumentCaptor.forClass(ScheduleShareRequest.class);

        verify(scheduleShareService).shareSchedule(
                eq(SCHEDULE_ID),
                requestCaptor.capture(),
                eq(USER_ID)
        );

        ScheduleShareRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.friendId())
                .isEqualTo(request.friendId());
    }

    @Test
    void 공유받은_일정_목록_조회에_성공하면_200과_일정목록을_반환한다() throws Exception {
        SharedScheduleResponse response = new SharedScheduleResponse(
                SHARE_ID,
                SCHEDULE_ID,
                "팀 회의",
                SCHEDULE_DATE,
                "일정소유자"
        );

        when(scheduleShareService.findAllSharedSchedules(USER_ID))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/schedule_shares/me")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(scheduleShareService)
                .findAllSharedSchedules(USER_ID);
    }

    @Test
    void 공유받은_일정_상세_조회에_성공하면_200과_상세정보를_반환한다() throws Exception {
        ScheduleParticipantResponse participant = new ScheduleParticipantResponse(
                USER_ID,
                "참가자",
                false,
                AttendanceStatus.ACCEPTED
        );

        SharedScheduleDetailResponse response =
                new SharedScheduleDetailResponse(
                        SHARE_ID,
                        SCHEDULE_ID,
                        "팀 회의",
                        "프로젝트 진행 상황을 확인합니다.",
                        SCHEDULE_DATE,
                        "일정소유자",
                        true,
                        1,
                        List.of(participant)
                );

        when(scheduleShareService.findOneSharedSchedule(
                eq(SHARE_ID),
                eq(USER_ID)
        )).thenReturn(response);

        mockMvc.perform(
                        get("/schedule_shares/{shareId}", SHARE_ID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.shareId").value(SHARE_ID))
                .andExpect(jsonPath("$.data.scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data.title").value("팀 회의"))
                .andExpect(jsonPath("$.data.contents")
                        .value("프로젝트 진행 상황을 확인합니다."))
                .andExpect(jsonPath("$.data.scheduleDate")
                        .value(SCHEDULE_DATE.toString()))
                .andExpect(jsonPath("$.data.ownerNickname")
                        .value("일정소유자"))
                .andExpect(jsonPath("$.data.participated").value(true))
                .andExpect(jsonPath("$.data.participantCount").value(1))
                .andExpect(jsonPath("$.data.participants").isArray())
                .andExpect(jsonPath("$.data.participants.length()").value(1))
                .andExpect(jsonPath("$.data.participants[0].userId")
                        .value(USER_ID))
                .andExpect(jsonPath("$.data.participants[0].nickname")
                        .value("참가자"))
                .andExpect(jsonPath("$.data.participants[0].owner")
                        .value(false))
                .andExpect(jsonPath("$.data.participants[0].attendanceStatus")
                        .value(AttendanceStatus.ACCEPTED.name()));

        verify(scheduleShareService)
                .findOneSharedSchedule(SHARE_ID, USER_ID);
    }

    @Test
    void 내가_공유한_일정_목록_조회에_성공하면_200과_목록을_반환한다() throws Exception {
        OwnedShareResponse response = new OwnedShareResponse(
                SHARE_ID,
                SCHEDULE_ID,
                "팀 회의",
                FRIEND_ID,
                "공유받은사용자"
        );

        when(scheduleShareService.findAllOwnedShares(USER_ID))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/schedule_shares/owned")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].shareId").value(SHARE_ID))
                .andExpect(jsonPath("$.data[0].scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data[0].title").value("팀 회의"))
                .andExpect(jsonPath("$.data[0].sharedUserId")
                        .value(FRIEND_ID))
                .andExpect(jsonPath("$.data[0].sharedUserNickname")
                        .value("공유받은사용자"));

        verify(scheduleShareService)
                .findAllOwnedShares(USER_ID);
    }

    @Test
    void 내가_공유한_일정_상세_조회에_성공하면_200과_상세정보를_반환한다() throws Exception {
        ScheduleParticipantResponse participant = new ScheduleParticipantResponse(
                FRIEND_ID,
                "공유받은사용자",
                false,
                AttendanceStatus.ACCEPTED
        );

        OwnedShareDetailResponse response =
                new OwnedShareDetailResponse(
                        SHARE_ID,
                        SCHEDULE_ID,
                        "팀 회의",
                        "프로젝트 진행 상황을 확인합니다.",
                        SCHEDULE_DATE,
                        FRIEND_ID,
                        "공유받은사용자",
                        true,
                        1,
                        List.of(participant)
                );

        when(scheduleShareService.findOneOwnedShareDetail(
                eq(SHARE_ID),
                eq(USER_ID)
        )).thenReturn(response);

        mockMvc.perform(
                        get("/schedule_shares/owned/{shareId}", SHARE_ID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.shareId").value(SHARE_ID))
                .andExpect(jsonPath("$.data.scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data.title").value("팀 회의"))
                .andExpect(jsonPath("$.data.content")
                        .value("프로젝트 진행 상황을 확인합니다."))
                .andExpect(jsonPath("$.data.scheduleDate")
                        .value(SCHEDULE_DATE.toString()))
                .andExpect(jsonPath("$.data.sharedUserId")
                        .value(FRIEND_ID))
                .andExpect(jsonPath("$.data.sharedUserNickname")
                        .value("공유받은사용자"))
                .andExpect(jsonPath("$.data.participated").value(true))
                .andExpect(jsonPath("$.data.participantCount").value(1))
                .andExpect(jsonPath("$.data.participants").isArray())
                .andExpect(jsonPath("$.data.participants.length()").value(1))
                .andExpect(jsonPath("$.data.participants[0].userId")
                        .value(FRIEND_ID))
                .andExpect(jsonPath("$.data.participants[0].nickname")
                        .value("공유받은사용자"))
                .andExpect(jsonPath("$.data.participants[0].owner")
                        .value(false))
                .andExpect(jsonPath("$.data.participants[0].attendanceStatus")
                        .value(AttendanceStatus.ACCEPTED.name()));

        verify(scheduleShareService)
                .findOneOwnedShareDetail(SHARE_ID, USER_ID);
    }

    @Test
    void 일정_참가자_목록_조회에_성공하면_200과_참가자목록을_반환한다() throws Exception {
        ScheduleParticipantResponse participant = new ScheduleParticipantResponse(
                FRIEND_ID,
                "참가자",
                false,
                AttendanceStatus.ACCEPTED
        );

        ScheduleParticipantListResponse response =
                new ScheduleParticipantListResponse(
                        1,
                        List.of(participant)
                );

        when(scheduleShareService.findParticipants(
                eq(USER_ID),
                eq(SCHEDULE_ID)
        )).thenReturn(response);

        mockMvc.perform(
                        get("/schedule_shares/{scheduleId}/participants", SCHEDULE_ID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.participantCount").value(1))
                .andExpect(jsonPath("$.data.participants").isArray())
                .andExpect(jsonPath("$.data.participants.length()").value(1))
                .andExpect(jsonPath("$.data.participants[0].userId")
                        .value(FRIEND_ID))
                .andExpect(jsonPath("$.data.participants[0].nickname")
                        .value("참가자"))
                .andExpect(jsonPath("$.data.participants[0].owner")
                        .value(false))
                .andExpect(jsonPath("$.data.participants[0].attendanceStatus")
                        .value(AttendanceStatus.ACCEPTED.name()));

        verify(scheduleShareService)
                .findParticipants(USER_ID, SCHEDULE_ID);
    }

    @Test
    void 참가_여부_변경에_성공하면_200과_수정_성공_응답을_반환한다() throws Exception {
        UpdateAttendanceRequest request =
                new UpdateAttendanceRequest(AttendanceStatus.ACCEPTED);

        doNothing().when(scheduleShareService).updateAttendance(
                eq(USER_ID),
                eq(SCHEDULE_ID),
                any(UpdateAttendanceRequest.class)
        );

        mockMvc.perform(
                        patch(
                                "/schedule_shares/{scheduleId}/attendance",
                                SCHEDULE_ID
                        )
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<UpdateAttendanceRequest> requestCaptor =
                ArgumentCaptor.forClass(UpdateAttendanceRequest.class);

        verify(scheduleShareService).updateAttendance(
                eq(USER_ID),
                eq(SCHEDULE_ID),
                requestCaptor.capture()
        );

        UpdateAttendanceRequest capturedRequest =
                requestCaptor.getValue();

        assertThat(capturedRequest.attendanceStatus())
                .isEqualTo(request.attendanceStatus());
    }

    @Test
    void 일정_공유_취소에_성공하면_200과_삭제_성공_응답을_반환한다() throws Exception {
        doNothing().when(scheduleShareService).cancelShare(
                eq(SHARE_ID),
                eq(USER_ID)
        );

        mockMvc.perform(
                        delete("/schedule_shares/{shareId}", SHARE_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.DELETE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.DELETE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(scheduleShareService)
                .cancelShare(SHARE_ID, USER_ID);
    }

    @Test
    void 일정_공유시_friendId가_null이면_400과_Validation_메시지를_반환한다()
            throws Exception {
        ScheduleShareRequest request =
                new ScheduleShareRequest(null);

        mockMvc.perform(
                        post("/schedule_shares/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("must not be null"));

        verifyNoInteractions(scheduleShareService);
    }

    @Test
    void 일정_공유시_friendId가_0이면_400과_Validation_메시지를_반환한다()
            throws Exception {
        ScheduleShareRequest request =
                new ScheduleShareRequest(0L);

        mockMvc.perform(
                        post("/schedule_shares/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("must be greater than 0"));

        verifyNoInteractions(scheduleShareService);
    }

    @Test
    void 일정_공유시_friendId가_음수이면_400과_Validation_메시지를_반환한다()
            throws Exception {
        ScheduleShareRequest request =
                new ScheduleShareRequest(-1L);

        mockMvc.perform(
                        post("/schedule_shares/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("must be greater than 0"));

        verifyNoInteractions(scheduleShareService);
    }

    @Test
    void 참가_여부_변경시_attendanceStatus가_null이면_400과_Validation_메시지를_반환한다()
            throws Exception {
        UpdateAttendanceRequest request =
                new UpdateAttendanceRequest(null);

        mockMvc.perform(
                        patch(
                                "/schedule_shares/{scheduleId}/attendance",
                                SCHEDULE_ID
                        )
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("must not be null"));

        verifyNoInteractions(scheduleShareService);
    }

    @Test
    void 잘못된_attendanceStatus_값이_들어오면_400과_잘못된_입력_응답을_반환한다()
            throws Exception {
        String request = """
                {
                    "attendanceStatus": "INVALID_STATUS"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/schedule_shares/{scheduleId}/attendance",
                                SCHEDULE_ID
                        )
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_INPUT.getStatus()));

        verifyNoInteractions(scheduleShareService);
    }

    @Test
    void 잘못된_scheduleId_형식이_들어오면_400과_잘못된_입력_응답을_반환한다()
            throws Exception {
        mockMvc.perform(
                        get("/schedule_shares/{scheduleId}/participants", "abc")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_INPUT.getStatus()));

        verifyNoInteractions(scheduleShareService);
    }

    @Test
    void 잘못된_shareId_형식이_들어오면_400과_잘못된_입력_응답을_반환한다()
            throws Exception {
        mockMvc.perform(
                        get("/schedule_shares/{shareId}", "abc")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status")
                        .value(ErrorEnum.INVALID_INPUT.getStatus()));

        verifyNoInteractions(scheduleShareService);
    }
}