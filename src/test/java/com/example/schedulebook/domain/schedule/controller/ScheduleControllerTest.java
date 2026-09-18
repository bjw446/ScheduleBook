package com.example.schedulebook.domain.schedule.controller;

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
import com.example.schedulebook.domain.schedule.dto.request.CreateScheduleRequest;
import com.example.schedulebook.domain.schedule.dto.request.UpdateScheduleRequest;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleDetailResponse;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.example.schedulebook.domain.schedule.service.ScheduleService;
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
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser(username = "1", roles = "USER")
class ScheduleControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    private static final LocalDate SCHEDULE_DATE = LocalDate.of(2026, 9, 18);
    private static final LocalTime START_TIME = LocalTime.of(13, 0);
    private static final LocalTime END_TIME = LocalTime.of(14, 0);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleService scheduleService;

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
        // 시큐리티 필터 체인이 MockMvc에 정확히 적용되도록 설정
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
    void 일정_생성에_성공하면_201과_생성된_일정을_반환한다() throws Exception {
        // given
        CreateScheduleRequest request = new CreateScheduleRequest(
                "팀 회의", "프로젝트 진행 상황을 확인합니다.", SCHEDULE_DATE, START_TIME, END_TIME
        );

        ScheduleSummaryResponse response = new ScheduleSummaryResponse(
                SCHEDULE_ID, "팀 회의", 0, SCHEDULE_DATE
        );

        when(scheduleService.createSchedule(any(CreateScheduleRequest.class), eq(USER_ID)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        post("/schedules")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.CREATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data.title").value("팀 회의"))
                .andExpect(jsonPath("$.data.commentCount").value(0))
                .andExpect(jsonPath("$.data.scheduleDate").value(SCHEDULE_DATE.toString()));

        ArgumentCaptor<CreateScheduleRequest> requestCaptor = ArgumentCaptor.forClass(CreateScheduleRequest.class);
        verify(scheduleService).createSchedule(requestCaptor.capture(), eq(USER_ID));

        CreateScheduleRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.title()).isEqualTo(request.title());
        assertThat(capturedRequest.content()).isEqualTo(request.content());
        assertThat(capturedRequest.scheduleDate()).isEqualTo(request.scheduleDate());
        assertThat(capturedRequest.startTime()).isEqualTo(request.startTime());
        assertThat(capturedRequest.endTime()).isEqualTo(request.endTime());
    }

    @Test
    void 일정_단건_조회에_성공하면_200과_상세정보를_반환한다() throws Exception {
        // given
        ScheduleDetailResponse response = new ScheduleDetailResponse(
                SCHEDULE_ID, "팀 회의", "프로젝트 진행 상황을 확인합니다.", 0,
                SCHEDULE_DATE, START_TIME, END_TIME, true, true, false, 0, List.of()
        );

        when(scheduleService.findOneSchedule(eq(SCHEDULE_ID), eq(USER_ID)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/schedules/{scheduleId}", SCHEDULE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data.title").value("팀 회의"))
                .andExpect(jsonPath("$.data.content").value("프로젝트 진행 상황을 확인합니다."))
                .andExpect(jsonPath("$.data.commentCount").value(0))
                .andExpect(jsonPath("$.data.scheduleDate").value(SCHEDULE_DATE.toString()))
                .andExpect(jsonPath("$.data.startTime").value("13:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("14:00:00"))
                .andExpect(jsonPath("$.data.startTimeSpecified").value(true))
                .andExpect(jsonPath("$.data.endTimeSpecified").value(true))
                .andExpect(jsonPath("$.data.participated").value(false))
                .andExpect(jsonPath("$.data.participantCount").value(0))
                .andExpect(jsonPath("$.data.participants").isArray())
                .andExpect(jsonPath("$.data.participants.length()").value(0));

        verify(scheduleService).findOneSchedule(eq(SCHEDULE_ID), eq(USER_ID));
    }

    @Test
    void 월별_일정_조회에_성공하면_200과_일정목록을_반환한다() throws Exception {
        // given
        int year = 2026;
        int month = 9;

        ScheduleSummaryResponse response = new ScheduleSummaryResponse(
                SCHEDULE_ID, "팀 회의", 0, SCHEDULE_DATE
        );

        when(scheduleService.findSchedulesByMonth(eq(year), eq(month), eq(USER_ID)))
                .thenReturn(List.of(response));

        // when & then
        mockMvc.perform(
                        get("/schedules")
                                .param("year", String.valueOf(year))
                                .param("month", String.valueOf(month))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data[0].title").value("팀 회의"))
                .andExpect(jsonPath("$.data[0].commentCount").value(0))
                .andExpect(jsonPath("$.data[0].scheduleDate").value(SCHEDULE_DATE.toString()));

        verify(scheduleService).findSchedulesByMonth(eq(year), eq(month), eq(USER_ID));
    }

    @Test
    void 날짜별_일정_조회에_성공하면_200과_일정목록을_반환한다() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 9, 18);

        ScheduleSummaryResponse response = new ScheduleSummaryResponse(
                SCHEDULE_ID, "팀 회의", 0, SCHEDULE_DATE
        );

        when(scheduleService.findSchedulesByDate(eq(date), eq(USER_ID)))
                .thenReturn(List.of(response));

        // when & then
        mockMvc.perform(
                        get("/schedules/date")
                                .param("date", date.toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data[0].title").value("팀 회의"))
                .andExpect(jsonPath("$.data[0].commentCount").value(0))
                .andExpect(jsonPath("$.data[0].scheduleDate").value(SCHEDULE_DATE.toString()));

        verify(scheduleService).findSchedulesByDate(eq(date), eq(USER_ID));
    }

    @Test
    void 일정_수정에_성공하면_200과_수정된_일정을_반환한다() throws Exception {
        // given
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                "수정된 팀 회의", "수정된 회의 내용입니다.", SCHEDULE_DATE, START_TIME, END_TIME
        );

        ScheduleSummaryResponse response = new ScheduleSummaryResponse(
                SCHEDULE_ID, "수정된 팀 회의", 0, SCHEDULE_DATE
        );

        when(scheduleService.updateSchedule(eq(SCHEDULE_ID), any(UpdateScheduleRequest.class), eq(USER_ID)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        put("/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data.title").value("수정된 팀 회의"))
                .andExpect(jsonPath("$.data.commentCount").value(0))
                .andExpect(jsonPath("$.data.scheduleDate").value(SCHEDULE_DATE.toString()));

        ArgumentCaptor<UpdateScheduleRequest> requestCaptor =
                ArgumentCaptor.forClass(UpdateScheduleRequest.class);

        verify(scheduleService).updateSchedule(
                eq(SCHEDULE_ID),
                requestCaptor.capture(),
                eq(USER_ID)
        );

        UpdateScheduleRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.title()).isEqualTo(request.title());
        assertThat(capturedRequest.content()).isEqualTo(request.content());
        assertThat(capturedRequest.scheduleDate()).isEqualTo(request.scheduleDate());
        assertThat(capturedRequest.startTime()).isEqualTo(request.startTime());
        assertThat(capturedRequest.endTime()).isEqualTo(request.endTime());
    }

    @Test
    void 일정_삭제에_성공하면_200과_삭제_성공_응답을_반환한다() throws Exception {
        // given - 삭제는 별도 리턴값이 없다면 doNothing 또는 생략 가능 (void 메서드 가정)
        doNothing().when(scheduleService).deleteSchedule(eq(SCHEDULE_ID), eq(USER_ID));

        // when & then
        mockMvc.perform(
                        delete("/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.DELETE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.DELETE_SUCCESS.getMessage()));

        verify(scheduleService).deleteSchedule(eq(SCHEDULE_ID), eq(USER_ID));
    }

    @Test
    void 일정_생성시_제목이_없으면_400과_Validation_메시지를_반환한다() throws Exception {
        CreateScheduleRequest request = new CreateScheduleRequest(
                null, "일정 내용", SCHEDULE_DATE, START_TIME, END_TIME
        );

        mockMvc.perform(
                        post("/schedules")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("일정 제목은 필수 입력사항 입니다."));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 일정_생성시_제목이_50자를_초과하면_400과_Validation_메시지를_반환한다() throws Exception {
        String title = "a".repeat(51);
        CreateScheduleRequest request = new CreateScheduleRequest(
                title, "일정 내용", SCHEDULE_DATE, START_TIME, END_TIME
        );

        mockMvc.perform(
                        post("/schedules")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("일정 제목은 최대 50자 까지 입력 가능합니다."));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 일정_생성시_내용이_없으면_400과_Validation_메시지를_반환한다() throws Exception {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "일정 제목", null, SCHEDULE_DATE, START_TIME, END_TIME
        );

        mockMvc.perform(
                        post("/schedules")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("일정 내용은 필수 입력사항 입니다."));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 일정_생성시_내용이_1000자를_초과하면_400과_Validation_메시지를_반환한다() throws Exception {
        String content = "a".repeat(1001);
        CreateScheduleRequest request = new CreateScheduleRequest(
                "일정 제목", content, SCHEDULE_DATE, START_TIME, END_TIME
        );

        mockMvc.perform(
                        post("/schedules")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("일정 내용은 최대 1000자 까지 입력 가능합니다."));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 일정_생성시_날짜가_없으면_400과_Validation_메시지를_반환한다() throws Exception {
        CreateScheduleRequest request = new CreateScheduleRequest(
                "일정 제목", "일정 내용", null, START_TIME, END_TIME
        );

        mockMvc.perform(
                        post("/schedules")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("일정 날짜는 필수 입력사항 입니다."));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 일정_수정시_제목이_없으면_400과_Validation_메시지를_반환한다() throws Exception {
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                null, "수정 내용", SCHEDULE_DATE, START_TIME, END_TIME
        );

        mockMvc.perform(
                        put("/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("일정 제목은 필수 입력사항 입니다."));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 일정_수정시_내용이_없으면_400과_Validation_메시지를_반환한다() throws Exception {
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                "수정 제목", null, SCHEDULE_DATE, START_TIME, END_TIME
        );

        mockMvc.perform(
                        put("/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("일정 내용은 필수 입력사항 입니다."));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 일정_수정시_날짜가_없으면_400과_Validation_메시지를_반환한다() throws Exception {
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                "수정 제목", "수정 내용", null, START_TIME, END_TIME
        );

        mockMvc.perform(
                        put("/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_ARGUMENT.getStatus()))
                .andExpect(jsonPath("$.message").value("일정 날짜는 필수 입력사항 입니다."));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 잘못된_날짜_형식이_들어오면_400과_잘못된_입력_응답을_반환한다() throws Exception {
        String request = """
                {
                    "title": "일정 제목",
                    "content": "일정 내용",
                    "scheduleDate": "2026-99-99",
                    "startTime": "13:00:00",
                    "endTime": "14:00:00"
                }
                """;

        mockMvc.perform(
                        post("/schedules")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_INPUT.getStatus()));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 잘못된_월별_조회_파라미터가_문자열이면_400을_반환한다() throws Exception {
        mockMvc.perform(
                        get("/schedules")
                                .param("year", "abc")
                                .param("month", "9")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_INPUT.getStatus()));

        verifyNoInteractions(scheduleService);
    }
}