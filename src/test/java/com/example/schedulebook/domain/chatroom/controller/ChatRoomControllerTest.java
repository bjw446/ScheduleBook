package com.example.schedulebook.domain.chatroom.controller;

import com.example.schedulebook.common.config.GlobalExceptionHandler;
import com.example.schedulebook.common.config.SecurityConfig;
import com.example.schedulebook.common.filter.RateLimitFilter;
import com.example.schedulebook.common.security.*;
import com.example.schedulebook.domain.chatroom.dto.request.ChatRoomInviteRequest;
import com.example.schedulebook.domain.chatroom.dto.request.ChatRoomUpdateNameRequest;
import com.example.schedulebook.domain.chatroom.dto.request.GroupChatRoomCreateRequest;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomCursor;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomDetailResponse;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomListResponse;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomResponse;
import com.example.schedulebook.domain.chatroom.dto.response.ChatRoomSliceResponse;
import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;
import com.example.schedulebook.domain.chatroom.service.ChatRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class ChatRoomControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long FRIEND_ID = 2L;
    private static final Long ROOM_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

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
    void setUp() throws Exception {
        doAnswer(invocation -> {
            var filterChain = invocation.getArgument(
                    2, FilterChain.class
            );

            filterChain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            var filterChain = invocation.getArgument(
                    2, FilterChain.class
            );

            filterChain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void createDirectRoom_성공하면_200_OK와_채팅방_정보를_반환한다() throws Exception {
        // given
        ChatRoomResponse response = new ChatRoomResponse(
                ROOM_ID,
                ChatRoomType.DIRECT,
                "상대방",
                2
        );

        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId)
                    .thenReturn(USER_ID);

            when(chatRoomService.createDirectRoom(USER_ID, FRIEND_ID))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(
                            post("/chat/rooms/direct/{friendId}", FRIEND_ID)
                                    .with(csrf())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                    .andExpect(jsonPath("$.data.chatRoomType").value("DIRECT"))
                    .andExpect(jsonPath("$.data.roomName").value("상대방"))
                    .andExpect(jsonPath("$.data.memberCount").value(2));

            verify(chatRoomService)
                    .createDirectRoom(USER_ID, FRIEND_ID);
        }
    }

    @Test
    @WithMockUser
    void createGroupRoom_성공하면_201_CREATED와_채팅방_정보를_반환한다() throws Exception {
        // given
        GroupChatRoomCreateRequest request =
                new GroupChatRoomCreateRequest(
                        "스터디방",
                        List.of(FRIEND_ID)
                );

        ChatRoomResponse response = new ChatRoomResponse(
                ROOM_ID,
                ChatRoomType.GROUP,
                "스터디방",
                2
        );

        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId)
                    .thenReturn(USER_ID);

            when(chatRoomService.createGroupRoom(USER_ID, request))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(
                            post("/chat/rooms/group")
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content("""
                                            {
                                                "name": "스터디방",
                                                "memberIds": [2]
                                            }
                                            """)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                    .andExpect(jsonPath("$.data.chatRoomType").value("GROUP"))
                    .andExpect(jsonPath("$.data.roomName").value("스터디방"))
                    .andExpect(jsonPath("$.data.memberCount").value(2));

            verify(chatRoomService)
                    .createGroupRoom(USER_ID, request);
        }
    }

    @Test
    @WithMockUser
    void createGroupRoom_name이_빈_문자열이면_400_BAD_REQUEST를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/chat/rooms/group")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "",
                                            "memberIds": [2]
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatRoomService);
    }

    @Test
    @WithMockUser
    void createGroupRoom_memberIds가_비어있으면_400_BAD_REQUEST를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/chat/rooms/group")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "스터디방",
                                            "memberIds": []
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatRoomService);
    }

    @Test
    @WithMockUser
    void inviteMembers_성공하면_200_OK와_채팅방_정보를_반환한다() throws Exception {
        // given
        ChatRoomInviteRequest request =
                new ChatRoomInviteRequest(List.of(FRIEND_ID));

        ChatRoomResponse response = new ChatRoomResponse(
                ROOM_ID,
                ChatRoomType.GROUP,
                "스터디방",
                3
        );

        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId)
                    .thenReturn(USER_ID);

            when(chatRoomService.inviteMembers(USER_ID, ROOM_ID, request))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(
                            post("/chat/rooms/{roomId}/invite", ROOM_ID)
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content("""
                                            {
                                                "memberIds": [2]
                                            }
                                            """)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                    .andExpect(jsonPath("$.data.chatRoomType").value("GROUP"))
                    .andExpect(jsonPath("$.data.roomName").value("스터디방"))
                    .andExpect(jsonPath("$.data.memberCount").value(3));

            verify(chatRoomService)
                    .inviteMembers(USER_ID, ROOM_ID, request);
        }
    }

    @Test
    @WithMockUser
    void inviteMembers_memberIds가_비어있으면_400_BAD_REQUEST를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/chat/rooms/{roomId}/invite", ROOM_ID)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                            "memberIds": []
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatRoomService);
    }

    @Test
    @WithMockUser
    void getMyChatRooms_첫_페이지를_조회하면_200_OK와_목록을_반환한다() throws Exception {
        // given
        ChatRoomListResponse room = new ChatRoomListResponse(
                ROOM_ID,
                "스터디방",
                "안녕하세요",
                LocalDateTime.of(2026, 9, 29, 10, 0),
                2
        );

        ChatRoomSliceResponse response = new ChatRoomSliceResponse(
                List.of(room),
                null,
                false
        );

        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId)
                    .thenReturn(USER_ID);

            when(chatRoomService.findMyChatRooms(
                    USER_ID,
                    null,
                    null,
                    30
            )).thenReturn(response);

            // when & then
            mockMvc.perform(
                            get("/chat/rooms")
                                    .with(csrf())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.chatRoomListResponses[0].roomId").value(ROOM_ID))
                    .andExpect(jsonPath("$.data.chatRoomListResponses[0].roomName").value("스터디방"))
                    .andExpect(jsonPath("$.data.chatRoomListResponses[0].lastMessage").value("안녕하세요"))
                    .andExpect(jsonPath("$.data.chatRoomListResponses[0].unreadCount").value(2))
                    .andExpect(jsonPath("$.data.hasNext").value(false));

            verify(chatRoomService)
                    .findMyChatRooms(USER_ID, null, null, 30);
        }
    }

    @Test
    @WithMockUser
    void getMyChatRooms_cursor와_size를_전달하면_Service에_그대로_전달한다() throws Exception {
        // given
        LocalDateTime cursorTime = LocalDateTime.of(2026, 9, 29, 10, 0);

        ChatRoomSliceResponse response = new ChatRoomSliceResponse(
                List.of(),
                new ChatRoomCursor(cursorTime, ROOM_ID),
                true
        );

        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId)
                    .thenReturn(USER_ID);

            when(chatRoomService.findMyChatRooms(
                    USER_ID,
                    cursorTime,
                    ROOM_ID,
                    10
            )).thenReturn(response);

            // when & then
            mockMvc.perform(
                            get("/chat/rooms")
                                    .with(csrf())
                                    .param("cursorTime", "2026-09-29T10:00:00")
                                    .param("cursorRoomId", ROOM_ID.toString())
                                    .param("size", "10")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.nextCursor.roomId").value(ROOM_ID))
                    .andExpect(jsonPath("$.data.nextCursor.lastMessageAt").value("2026-09-29T10:00:00"));

            verify(chatRoomService)
                    .findMyChatRooms(USER_ID, cursorTime, ROOM_ID, 10);
        }
    }

    @Test
    @WithMockUser
    void getChatRoom_성공하면_200_OK와_상세정보를_반환한다() throws Exception {
        // given
        ChatRoomDetailResponse response = new ChatRoomDetailResponse(
                ROOM_ID,
                "스터디방",
                ChatRoomType.GROUP,
                2,
                100L,
                LocalDateTime.of(2026, 9, 29, 9, 0),
                200L,
                "안녕하세요",
                LocalDateTime.of(2026, 9, 29, 10, 0),
                1,
                List.of()
        );

        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId)
                    .thenReturn(USER_ID);

            when(chatRoomService.findChatRoom(USER_ID, ROOM_ID))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(
                            get("/chat/rooms/{roomId}", ROOM_ID)
                                    .with(csrf())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                    .andExpect(jsonPath("$.data.roomName").value("스터디방"))
                    .andExpect(jsonPath("$.data.chatRoomType").value("GROUP"))
                    .andExpect(jsonPath("$.data.memberCount").value(2))
                    .andExpect(jsonPath("$.data.lastReadMessageId").value(100))
                    .andExpect(jsonPath("$.data.lastMessageId").value(200))
                    .andExpect(jsonPath("$.data.lastMessage").value("안녕하세요"))
                    .andExpect(jsonPath("$.data.unreadCount").value(1));

            verify(chatRoomService)
                    .findChatRoom(USER_ID, ROOM_ID);
        }
    }

    @Test
    @WithMockUser
    void updateRoomName_성공하면_200_OK와_수정된_채팅방_정보를_반환한다() throws Exception {
        // given
        ChatRoomUpdateNameRequest request =
                new ChatRoomUpdateNameRequest("새 채팅방");

        ChatRoomResponse response = new ChatRoomResponse(
                ROOM_ID,
                ChatRoomType.GROUP,
                "새 채팅방",
                2
        );

        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId)
                    .thenReturn(USER_ID);

            when(chatRoomService.updateRoomName(USER_ID, ROOM_ID, request))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(
                            patch("/chat/rooms/{roomId}/name", ROOM_ID)
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content("""
                                            {
                                                "name": "새 채팅방"
                                            }
                                            """)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roomId").value(ROOM_ID))
                    .andExpect(jsonPath("$.data.roomName").value("새 채팅방"));

            verify(chatRoomService)
                    .updateRoomName(USER_ID, ROOM_ID, request);
        }
    }

    @Test
    @WithMockUser
    void updateRoomName_name이_빈_문자열이면_400_BAD_REQUEST를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        patch("/chat/rooms/{roomId}/name", ROOM_ID)
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatRoomService);
    }

    @Test
    @WithMockUser
    void leaveChatRoom_성공하면_200_OK와_DELETE_SUCCESS를_반환한다() throws Exception {
        // given
        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId)
                    .thenReturn(USER_ID);

            // when & then
            mockMvc.perform(
                            delete("/chat/rooms/{roomId}/leave", ROOM_ID)
                                    .with(csrf())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(chatRoomService)
                    .leaveChatRoom(USER_ID, ROOM_ID);
        }
    }
}