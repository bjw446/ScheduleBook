package com.example.schedulebook.domain.admin.service;

import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.auth.service.SessionService;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.validator.UserValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private UserValidator userValidator;

    @Mock
    private OutboxService outboxService;

    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private AdminSessionService adminSessionService;

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String SESSION_ID = "session-123";

    @Test
    void 사용자의_전체_세션을_조회하면_세션_목록을_반환한다() {
        // given
        LocalDateTime loginAt =
                LocalDateTime.of(2026, 9, 15, 10, 0);

        LocalDateTime lastAccessAt =
                LocalDateTime.of(2026, 9, 15, 11, 0);

        List<SessionInfoResponse> sessions = List.of(
                new SessionInfoResponse(
                        "session-1",
                        "127.0.0.1",
                        "Mozilla/5.0",
                        loginAt,
                        lastAccessAt
                )
        );

        when(sessionService.findSessions(USER_ID))
                .thenReturn(sessions);

        // when
        List<SessionInfoResponse> result =
                adminSessionService.findAllUserSessions(
                        ADMIN_ID,
                        USER_ID
                );

        // then
        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator).validateActiveUser(USER_ID);
        verify(sessionService).findSessions(USER_ID);

        assertThat(result).hasSize(1);

        SessionInfoResponse response = result.get(0);

        assertThat(response.sessionId())
                .isEqualTo("session-1");
        assertThat(response.ip())
                .isEqualTo("127.0.0.1");
        assertThat(response.userAgent())
                .isEqualTo("Mozilla/5.0");
        assertThat(response.loginAt())
                .isEqualTo(loginAt);
        assertThat(response.lastAccessAt())
                .isEqualTo(lastAccessAt);
    }

    @Test
    void 관리자_권한_검증에_실패하면_사용자_세션을_조회하지_않는다() {
        // given
        RuntimeException exception = new RuntimeException("admin validation failed");

        doThrow(exception)
                .when(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        // when & then
        assertThatThrownBy(() ->
                adminSessionService.findAllUserSessions(
                        ADMIN_ID,
                        USER_ID
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator, never()).validateActiveUser(USER_ID);
        verifyNoInteractions(sessionService);
    }

    @Test
    void 사용자_상태_검증에_실패하면_세션을_조회하지_않는다() {
        // given
        RuntimeException exception = new RuntimeException("user validation failed");

        doThrow(exception)
                .when(userValidator)
                .validateActiveUser(USER_ID);

        // when & then
        assertThatThrownBy(() ->
                adminSessionService.findAllUserSessions(
                        ADMIN_ID,
                        USER_ID
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator).validateActiveUser(USER_ID);
        verifyNoInteractions(sessionService);
    }

    @Test
    void 사용자의_특정_세션을_강제_로그아웃하면_세션_로그아웃과_Audit_Outbox를_저장한다() {
        // given
        String ip = "127.0.0.1";
        String userAgent = "Mozilla/5.0";

        when(servletRequest.getRemoteAddr())
                .thenReturn(ip);
        when(servletRequest.getHeader("User-Agent"))
                .thenReturn(userAgent);

        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<AuditEvent> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        // when
        adminSessionService.logoutUserOneSession(
                ADMIN_ID,
                USER_ID,
                SESSION_ID,
                servletRequest
        );

        // then
        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator).validateActiveUser(USER_ID);

        verify(sessionService).forceLogoutSession(
                USER_ID,
                SESSION_ID
        );

        verify(outboxService).save(
                eventIdCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(OutboxAggregateType.USER),
                org.mockito.ArgumentMatchers.eq(String.valueOf(USER_ID)),
                org.mockito.ArgumentMatchers.eq(OutboxEventType.AUDIT_EVENT),
                auditEventCaptor.capture()
        );

        String eventId = eventIdCaptor.getValue();
        AuditEvent auditEvent = auditEventCaptor.getValue();

        assertThat(eventId).isNotBlank();

        assertThat(auditEvent.eventId())
                .isEqualTo(eventId);
        assertThat(auditEvent.userId())
                .isEqualTo(USER_ID);
        assertThat(auditEvent.adminId())
                .isEqualTo(ADMIN_ID);
        assertThat(auditEvent.loginId())
                .isNull();
        assertThat(auditEvent.eventType())
                .isEqualTo(AuditEventType.FORCE_LOGOUT);
        assertThat(auditEvent.ip())
                .isEqualTo(ip);
        assertThat(auditEvent.userAgent())
                .isEqualTo(userAgent);
    }

    @Test
    void 특정_세션_로그아웃에서_IP와_UserAgent가_없으면_UNKNOWN으로_저장한다() {
        // given
        when(servletRequest.getRemoteAddr())
                .thenReturn(null);
        when(servletRequest.getHeader("User-Agent"))
                .thenReturn(" ");

        ArgumentCaptor<AuditEvent> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        // when
        adminSessionService.logoutUserOneSession(
                ADMIN_ID,
                USER_ID,
                SESSION_ID,
                servletRequest
        );

        // then
        verify(outboxService).save(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(OutboxAggregateType.USER),
                org.mockito.ArgumentMatchers.eq(String.valueOf(USER_ID)),
                org.mockito.ArgumentMatchers.eq(OutboxEventType.AUDIT_EVENT),
                auditEventCaptor.capture()
        );

        AuditEvent auditEvent = auditEventCaptor.getValue();

        assertThat(auditEvent.ip())
                .isEqualTo("UNKNOWN");
        assertThat(auditEvent.userAgent())
                .isEqualTo("UNKNOWN");
    }

    @Test
    void 특정_세션_로그아웃에서_관리자_검증에_실패하면_로그아웃하지_않는다() {
        // given
        RuntimeException exception =
                new RuntimeException("admin validation failed");

        doThrow(exception)
                .when(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        // when & then
        assertThatThrownBy(() ->
                adminSessionService.logoutUserOneSession(
                        ADMIN_ID,
                        USER_ID,
                        SESSION_ID,
                        servletRequest
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator, never()).validateActiveUser(USER_ID);
        verifyNoInteractions(sessionService, outboxService, servletRequest);
    }

    @Test
    void 특정_세션_로그아웃에서_사용자_검증에_실패하면_로그아웃하지_않는다() {
        // given
        RuntimeException exception =
                new RuntimeException("user validation failed");

        doThrow(exception)
                .when(userValidator)
                .validateActiveUser(USER_ID);

        // when & then
        assertThatThrownBy(() ->
                adminSessionService.logoutUserOneSession(
                        ADMIN_ID,
                        USER_ID,
                        SESSION_ID,
                        servletRequest
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator).validateActiveUser(USER_ID);

        verifyNoInteractions(
                sessionService,
                outboxService,
                servletRequest
        );
    }

    @Test
    void 특정_세션_강제_로그아웃에_실패하면_Audit_Outbox를_저장하지_않는다() {
        // given
        RuntimeException exception =
                new RuntimeException("force logout failed");

        doThrow(exception)
                .when(sessionService)
                .forceLogoutSession(USER_ID, SESSION_ID);

        // when & then
        assertThatThrownBy(() ->
                adminSessionService.logoutUserOneSession(
                        ADMIN_ID,
                        USER_ID,
                        SESSION_ID,
                        servletRequest
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator).validateActiveUser(USER_ID);

        verify(sessionService)
                .forceLogoutSession(USER_ID, SESSION_ID);

        verify(outboxService, never()).save(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void 사용자의_전체_세션을_강제_로그아웃하면_모든_세션_로그아웃과_Audit_Outbox를_저장한다() {
        // given
        String ip = "192.168.0.10";
        String userAgent = "Chrome";

        when(servletRequest.getRemoteAddr())
                .thenReturn(ip);
        when(servletRequest.getHeader("User-Agent"))
                .thenReturn(userAgent);

        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<AuditEvent> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        // when
        adminSessionService.logoutUserAllSession(
                ADMIN_ID,
                USER_ID,
                servletRequest
        );

        // then
        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator).validateActiveUser(USER_ID);

        verify(sessionService)
                .forceLogoutAllSessions(USER_ID);

        verify(outboxService).save(
                eventIdCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(OutboxAggregateType.USER),
                org.mockito.ArgumentMatchers.eq(String.valueOf(USER_ID)),
                org.mockito.ArgumentMatchers.eq(OutboxEventType.AUDIT_EVENT),
                auditEventCaptor.capture()
        );

        String eventId = eventIdCaptor.getValue();
        AuditEvent auditEvent = auditEventCaptor.getValue();

        assertThat(eventId).isNotBlank();

        assertThat(auditEvent.eventId())
                .isEqualTo(eventId);
        assertThat(auditEvent.userId())
                .isEqualTo(USER_ID);
        assertThat(auditEvent.adminId())
                .isEqualTo(ADMIN_ID);
        assertThat(auditEvent.loginId())
                .isNull();
        assertThat(auditEvent.eventType())
                .isEqualTo(AuditEventType.FORCE_LOGOUT_ALL);
        assertThat(auditEvent.ip())
                .isEqualTo(ip);
        assertThat(auditEvent.userAgent())
                .isEqualTo(userAgent);
    }

    @Test
    void 전체_세션_로그아웃에서_IP와_UserAgent가_없으면_UNKNOWN으로_저장한다() {
        // given
        when(servletRequest.getRemoteAddr())
                .thenReturn("");
        when(servletRequest.getHeader("User-Agent"))
                .thenReturn(null);

        ArgumentCaptor<AuditEvent> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        // when
        adminSessionService.logoutUserAllSession(
                ADMIN_ID,
                USER_ID,
                servletRequest
        );

        // then
        verify(outboxService).save(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(OutboxAggregateType.USER),
                org.mockito.ArgumentMatchers.eq(String.valueOf(USER_ID)),
                org.mockito.ArgumentMatchers.eq(OutboxEventType.AUDIT_EVENT),
                auditEventCaptor.capture()
        );

        AuditEvent auditEvent = auditEventCaptor.getValue();

        assertThat(auditEvent.ip())
                .isEqualTo("UNKNOWN");
        assertThat(auditEvent.userAgent())
                .isEqualTo("UNKNOWN");
    }

    @Test
    void 전체_세션_로그아웃에서_관리자_검증에_실패하면_로그아웃하지_않는다() {
        // given
        RuntimeException exception =
                new RuntimeException("admin validation failed");

        doThrow(exception)
                .when(userValidator)
                .validateActiveAdmin(ADMIN_ID);

        // when & then
        assertThatThrownBy(() ->
                adminSessionService.logoutUserAllSession(
                        ADMIN_ID,
                        USER_ID,
                        servletRequest
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator, never()).validateActiveUser(USER_ID);

        verifyNoInteractions(
                sessionService,
                outboxService,
                servletRequest
        );
    }

    @Test
    void 전체_세션_로그아웃에서_사용자_검증에_실패하면_로그아웃하지_않는다() {
        // given
        RuntimeException exception =
                new RuntimeException("user validation failed");

        doThrow(exception)
                .when(userValidator)
                .validateActiveUser(USER_ID);

        // when & then
        assertThatThrownBy(() ->
                adminSessionService.logoutUserAllSession(
                        ADMIN_ID,
                        USER_ID,
                        servletRequest
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator).validateActiveUser(USER_ID);

        verifyNoInteractions(
                sessionService,
                outboxService,
                servletRequest
        );
    }

    @Test
    void 전체_세션_강제_로그아웃에_실패하면_Audit_Outbox를_저장하지_않는다() {
        // given
        RuntimeException exception =
                new RuntimeException("force logout all failed");

        doThrow(exception)
                .when(sessionService)
                .forceLogoutAllSessions(USER_ID);

        // when & then
        assertThatThrownBy(() ->
                adminSessionService.logoutUserAllSession(
                        ADMIN_ID,
                        USER_ID,
                        servletRequest
                )
        ).isSameAs(exception);

        verify(userValidator).validateActiveAdmin(ADMIN_ID);
        verify(userValidator).validateActiveUser(USER_ID);

        verify(sessionService)
                .forceLogoutAllSessions(USER_ID);

        verify(outboxService, never()).save(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}