package com.example.schedulebook.common.websocket.publisher;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    private WebSocketPublisher publisher;

    private final String destination = "/topic/schedule/1";
    private final Object payload = "test-payload";

    @BeforeEach
    void setUp() {
        publisher = new WebSocketPublisher(
                simpMessagingTemplate,
                afterCommitExecutor
        );
    }

    @Test
    void after_commit_전송을_AfterCommitExecutor에_위임한다() {
        // when
        publisher.sendAfterCommit(destination, payload);

        // then
        verify(afterCommitExecutor)
                .execute(any(Runnable.class));

        verifyNoInteractions(simpMessagingTemplate);
    }

    @Test
    void after_commit_작업_실행_시_destination과_payload를_전달한다() {
        // given
        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);

        publisher.sendAfterCommit(destination, payload);

        verify(afterCommitExecutor)
                .execute(runnableCaptor.capture());

        // when
        runnableCaptor.getValue().run();

        // then
        verify(simpMessagingTemplate)
                .convertAndSend(destination, payload);
    }

    @Test
    void after_commit_웹소켓_전송_중_예외가_발생해도_전파하지_않는다() {
        // given
        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);

        doThrow(new RuntimeException("WebSocket send failed"))
                .when(simpMessagingTemplate)
                .convertAndSend(destination, payload);

        publisher.sendAfterCommit(destination, payload);

        verify(afterCommitExecutor)
                .execute(runnableCaptor.capture());

        // when & then
        assertDoesNotThrow(
                () -> runnableCaptor.getValue().run()
        );

        verify(simpMessagingTemplate)
                .convertAndSend(destination, payload);
    }

    @Test
    void 즉시_전송_시_destination과_payload를_전달한다() {
        // when
        publisher.send(destination, payload);

        // then
        verify(simpMessagingTemplate)
                .convertAndSend(destination, payload);
    }

    @Test
    void 즉시_웹소켓_전송_중_예외가_발생해도_전파하지_않는다() {
        // given
        doThrow(new RuntimeException("WebSocket send failed"))
                .when(simpMessagingTemplate)
                .convertAndSend(destination, payload);

        // when & then
        assertDoesNotThrow(
                () -> publisher.send(destination, payload)
        );

        verify(simpMessagingTemplate)
                .convertAndSend(destination, payload);
    }
}