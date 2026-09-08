package com.example.schedulebook.domain.auth.handler;

import com.example.schedulebook.domain.auth.dispatcher.ForceLogoutDispatcher;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForceLogoutHandlerTest {

    @Mock
    private ForceLogoutDispatcher forceLogoutDispatcher;

    @Mock
    private ForceLogoutRetryService forceLogoutRetryService;

    @InjectMocks
    private ForceLogoutHandler forceLogoutHandler;

    @Test
    void given강제로그아웃이벤트_whenHandle_thenDispatcher호출() {
        // given
        ForceLogoutSessionEvent event = new ForceLogoutSessionEvent(
                "event-123",
                1L,
                "session-123",
                60_000L
        );

        // when
        forceLogoutHandler.handle(event);

        // then
        verify(forceLogoutDispatcher)
                .dispatch(event);

        verifyNoInteractions(forceLogoutRetryService);
    }

    @Test
    void givenDispatcher처리실패_whenHandle_then예외전파없이재시도정보저장() {
        // given
        ForceLogoutSessionEvent event = new ForceLogoutSessionEvent(
                "event-123",
                1L,
                "session-123",
                60_000L
        );

        RuntimeException exception =
                new RuntimeException("WebSocket send failed");

        doThrow(exception)
                .when(forceLogoutDispatcher)
                .dispatch(event);

        // when
        forceLogoutHandler.handle(event);

        // then
        verify(forceLogoutDispatcher)
                .dispatch(event);

        verify(forceLogoutRetryService)
                .save(event, exception.getMessage());
    }
}