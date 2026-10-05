package com.example.schedulebook.domain.comment.controller;

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
import com.example.schedulebook.domain.comment.dto.request.CreateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.request.UpdateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.response.ScheduleCommentListResponse;
import com.example.schedulebook.domain.comment.service.CommentService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CustomAuthenticationEntryPoint.class
})
class CommentControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long COMMENT_ID = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

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
    void createScheduleComment_유효한_요청이면_댓글을_생성하고_201을_반환한다() throws Exception {
        // given
        String request = """
                {
                    "content": "댓글 내용",
                    "parentCommentId": null
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/comments/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .with(authenticated())
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.CREATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(commentService)
                .createComment(
                        eq(USER_ID),
                        eq(SCHEDULE_ID),
                        any(CreateScheduleCommentRequest.class)
                );
    }

    @Test
    void getAllComment_인증된_사용자면_댓글_목록을_조회하고_200을_반환한다() throws Exception {
        // given
        ScheduleCommentListResponse response =
                new ScheduleCommentListResponse(
                        SCHEDULE_ID,
                        0,
                        List.of()
                );

        when(commentService.findAllComment(USER_ID, SCHEDULE_ID))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        get("/comments/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.scheduleId").value(SCHEDULE_ID))
                .andExpect(jsonPath("$.data.commentCount").value(0))
                .andExpect(jsonPath("$.data.comments").isArray())
                .andExpect(jsonPath("$.data.comments").isEmpty());

        verify(commentService)
                .findAllComment(USER_ID, SCHEDULE_ID);
    }

    @Test
    void updateScheduleComment_유효한_요청이면_댓글을_수정하고_200을_반환한다() throws Exception {
        // given
        String request = """
                {
                    "content": "수정된 댓글"
                }
                """;

        // when & then
        mockMvc.perform(
                        patch("/comments/{commentId}", COMMENT_ID)
                                .with(csrf())
                                .with(authenticated())
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(commentService)
                .updateComment(
                        eq(USER_ID),
                        eq(COMMENT_ID),
                        any(UpdateScheduleCommentRequest.class)
                );
    }

    @Test
    void deleteScheduleComment_인증된_사용자면_댓글을_삭제하고_200을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/comments/{commentId}", COMMENT_ID)
                                .with(csrf())
                                .with(authenticated())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.DELETE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.DELETE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(commentService)
                .deleteComment(USER_ID, COMMENT_ID);
    }

    @Test
    void createScheduleComment_content가_비어있으면_400을_반환한다() throws Exception {
        // given
        String request = """
                {
                    "content": "",
                    "parentCommentId": null
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/comments/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .with(authenticated())
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_INPUT.getStatus()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(commentService);
    }

    @Test
    void updateScheduleComment_content가_500자를_초과하면_400을_반환한다() throws Exception {
        // given
        String content = "a".repeat(501);

        String request = """
                {
                    "content": "%s"
                }
                """.formatted(content);

        // when & then
        mockMvc.perform(
                        patch("/comments/{commentId}", COMMENT_ID)
                                .with(csrf())
                                .with(authenticated())
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.INVALID_INPUT.getStatus()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(commentService);
    }

    @Test
    void createScheduleComment_인증되지_않은_사용자면_401과_공통_에러_응답을_반환한다() throws Exception {
        // given
        String request = """
                {
                    "content": "댓글 내용",
                    "parentCommentId": null
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/comments/schedules/{scheduleId}", SCHEDULE_ID)
                                .with(csrf())
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.UNAUTHORIZED.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.UNAUTHORIZED.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(commentService);
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