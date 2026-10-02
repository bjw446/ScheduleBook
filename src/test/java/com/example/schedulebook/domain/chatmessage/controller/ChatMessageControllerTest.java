package com.example.schedulebook.domain.chatmessage.controller;

import com.example.schedulebook.common.config.GlobalExceptionHandler;
import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.security.CustomAccessDeniedHandler;
import com.example.schedulebook.common.security.CustomAuthenticationEntryPoint;
import com.example.schedulebook.common.security.JwtAuthenticationFilter;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.common.security.UserPrincipal;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageScheduleShareRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageSearchRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatReadRequest;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageSliceResponse;
import com.example.schedulebook.domain.chatmessage.enums.ChatMessageType;
import com.example.schedulebook.domain.chatmessage.service.ChatMessageService;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.SchedulePreviewDetailResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotDiffResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotFieldChangeResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotHistoryResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatMessageController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class ChatMessageControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;
    private static final Long MESSAGE_ID = 100L;
    private static final Long SCHEDULE_ID = 200L;
    private static final Long LAST_READ_MESSAGE_ID = 90L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatMessageService chatMessageService;

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
    void shareSchedule_성공하면_201_CREATED와_성공_응답을_반환한다() throws Exception {
        // given
        ChatMessageScheduleShareRequest request =
                new ChatMessageScheduleShareRequest(
                        ROOM_ID,
                        SCHEDULE_ID
                );

        // when & then
        mockMvc.perform(
                        post("/chat/messages/schedule")
                                .with(csrf())
                                .with(authenticated())
                                .contentType("application/json")
                                .content("""
                                        {
                                            "roomId": 10,
                                            "scheduleId": 200
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.CREATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(chatMessageService)
                .shareSchedule(USER_ID, request);
    }

    @Test
    void acceptSharedSchedule_성공하면_201_CREATED와_성공_응답을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        post(
                                "/chat/messages/{messageId}/shared-schedule/accept",
                                MESSAGE_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.CREATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(chatMessageService)
                .acceptSharedSchedule(USER_ID, MESSAGE_ID);
    }

    @Test
    void getMessages_첫_페이지를_조회하면_200_OK와_메시지_목록을_반환한다() throws Exception {
        // given
        ChatMessageResponse message = new ChatMessageResponse(
                MESSAGE_ID,
                ROOM_ID,
                2L,
                "상대방",
                "안녕하세요",
                ChatMessageType.TEXT,
                null,
                false,
                1,
                LocalDateTime.of(2026, 9, 29, 10, 0),
                null
        );

        ChatMessageSliceResponse response =
                new ChatMessageSliceResponse(
                        List.of(message),
                        null,
                        false
                );

        when(chatMessageService.findMessages(
                USER_ID,
                ROOM_ID,
                new ChatMessageSearchRequest(null, 30)
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        get(
                                "/chat/messages/{roomId}/messages",
                                ROOM_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.messages[0].messageId")
                        .value(MESSAGE_ID))
                .andExpect(jsonPath("$.data.messages[0].roomId")
                        .value(ROOM_ID))
                .andExpect(jsonPath("$.data.messages[0].senderId")
                        .value(2))
                .andExpect(jsonPath("$.data.messages[0].senderNickname")
                        .value("상대방"))
                .andExpect(jsonPath("$.data.messages[0].content")
                        .value("안녕하세요"))
                .andExpect(jsonPath("$.data.messages[0].chatMessageType")
                        .value("TEXT"))
                .andExpect(jsonPath("$.data.messages[0].edited")
                        .value(false))
                .andExpect(jsonPath("$.data.messages[0].unreadMemberCount")
                        .value(1))
                .andExpect(jsonPath("$.data.messages[0].createdAt")
                        .value("2026-09-29T10:00:00"))
                .andExpect(jsonPath("$.data.messages[0].replyMessageResponse")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.hasNext")
                        .value(false))
                .andExpect(jsonPath("$.data.nextCursor")
                        .doesNotExist());

        verify(chatMessageService)
                .findMessages(
                        USER_ID,
                        ROOM_ID,
                        new ChatMessageSearchRequest(null, 30)
                );
    }

    @Test
    void getMessages_cursor와_size를_전달하면_Service에_그대로_전달한다() throws Exception {
        // given
        ChatMessageSearchRequest request =
                new ChatMessageSearchRequest(
                        LAST_READ_MESSAGE_ID,
                        10
                );

        ChatMessageSliceResponse response =
                new ChatMessageSliceResponse(
                        List.of(),
                        80L,
                        true
                );

        when(chatMessageService.findMessages(
                USER_ID,
                ROOM_ID,
                request
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        get(
                                "/chat/messages/{roomId}/messages",
                                ROOM_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                                .param(
                                        "cursor",
                                        LAST_READ_MESSAGE_ID.toString()
                                )
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.messages")
                        .isEmpty())
                .andExpect(jsonPath("$.data.nextCursor")
                        .value(80))
                .andExpect(jsonPath("$.data.hasNext")
                        .value(true));

        verify(chatMessageService)
                .findMessages(
                        USER_ID,
                        ROOM_ID,
                        request
                );
    }

    @Test
    void getSharedSchedule_성공하면_200_OK와_공유_일정_정보를_반환한다() throws Exception {
        // given
        SchedulePreviewDetailResponse response =
                new SchedulePreviewDetailResponse(
                        MESSAGE_ID,
                        SCHEDULE_ID,
                        "팀 회의",
                        "프로젝트 회의입니다.",
                        LocalDate.of(2026, 10, 1),
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        false,
                        false,
                        false,
                        true,
                        3L,
                        LocalDateTime.of(2026, 9, 30, 12, 0)
                );

        when(chatMessageService.findSharedSchedule(
                USER_ID,
                MESSAGE_ID
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        get(
                                "/chat/messages/{messageId}/shared-schedule",
                                MESSAGE_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.messageId")
                        .value(MESSAGE_ID))
                .andExpect(jsonPath("$.data.scheduleId")
                        .value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data.title")
                        .value("팀 회의"))
                .andExpect(jsonPath("$.data.content")
                        .value("프로젝트 회의입니다."))
                .andExpect(jsonPath("$.data.scheduleDate")
                        .value("2026-10-01"))
                .andExpect(jsonPath("$.data.startTime")
                        .value("14:00:00"))
                .andExpect(jsonPath("$.data.endTime")
                        .value("15:00:00"))
                .andExpect(jsonPath("$.data.deleted")
                        .value(false))
                .andExpect(jsonPath("$.data.canceled")
                        .value(false))
                .andExpect(jsonPath("$.data.edited")
                        .value(false))
                .andExpect(jsonPath("$.data.shared")
                        .value(true))
                .andExpect(jsonPath("$.data.scheduleVersion")
                        .value(3))
                .andExpect(jsonPath("$.data.scheduleUpdatedAt")
                        .value("2026-09-30T12:00:00"));

        verify(chatMessageService)
                .findSharedSchedule(USER_ID, MESSAGE_ID);
    }

    @Test
    void getScheduleSnapshotHistory_성공하면_200_OK와_이력을_반환한다() throws Exception {
        // given
        List<ScheduleSnapshotHistoryResponse> response =
                List.of();

        when(chatMessageService.findScheduleSnapshotHistory(
                USER_ID,
                MESSAGE_ID
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        get(
                                "/chat/messages/{messageId}/shared-schedule/history",
                                MESSAGE_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(chatMessageService)
                .findScheduleSnapshotHistory(USER_ID, MESSAGE_ID);
    }

    @Test
    void getScheduleSnapshotDiff_성공하면_200_OK와_변경사항을_반환한다() throws Exception {
        // given
        ScheduleSnapshotFieldChangeResponse change =
                new ScheduleSnapshotFieldChangeResponse(
                        "title",
                        "기존 제목",
                        "변경된 제목"
                );

        ScheduleSnapshotDiffResponse response =
                new ScheduleSnapshotDiffResponse(
                        1L,
                        2L,
                        List.of(change)
                );

        when(chatMessageService.findScheduleSnapshotDiff(
                USER_ID,
                MESSAGE_ID,
                1L,
                2L
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        get(
                                "/chat/messages/{messageId}/shared-schedule/diff",
                                MESSAGE_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                                .param("from", "1")
                                .param("to", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.fromVersion")
                        .value(1))
                .andExpect(jsonPath("$.data.toVersion")
                        .value(2))
                .andExpect(jsonPath("$.data.changes[0].field")
                        .value("title"))
                .andExpect(jsonPath("$.data.changes[0].before")
                        .value("기존 제목"))
                .andExpect(jsonPath("$.data.changes[0].after")
                        .value("변경된 제목"));

        verify(chatMessageService)
                .findScheduleSnapshotDiff(
                        USER_ID,
                        MESSAGE_ID,
                        1L,
                        2L
                );
    }

    @Test
    void readMessage_성공하면_200_OK와_성공_응답을_반환한다() throws Exception {
        // given
        ChatReadRequest request =
                new ChatReadRequest(LAST_READ_MESSAGE_ID);

        // when & then
        mockMvc.perform(
                        post(
                                "/chat/messages/{roomId}/read",
                                ROOM_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                                .contentType("application/json")
                                .content("""
                                        {
                                            "lastReadMessageId": 90
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(chatMessageService)
                .readMessage(
                        USER_ID,
                        ROOM_ID,
                        request.lastReadMessageId()
                );
    }

    @Test
    void cancelScheduleShare_성공하면_200_OK와_성공_응답을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        patch(
                                "/chat/messages/{messageId}/schedule/cancel",
                                MESSAGE_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(chatMessageService)
                .cancelScheduleShare(USER_ID, MESSAGE_ID);
    }

    @Test
    void deleteMessage_성공하면_200_OK와_성공_응답을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        delete(
                                "/chat/messages/{roomId}/delete/{messageId}",
                                ROOM_ID,
                                MESSAGE_ID
                        )
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status")
                        .value(SuccessEnum.DELETE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message")
                        .value(SuccessEnum.DELETE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(chatMessageService)
                .deleteMessage(
                        USER_ID,
                        ROOM_ID,
                        MESSAGE_ID
                );
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